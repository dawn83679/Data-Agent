package edu.zsc.ai.plugin.model.command.sql;

import edu.zsc.ai.plugin.capability.CommandExecutor;
import edu.zsc.ai.plugin.value.JdbcValueContext;
import edu.zsc.ai.plugin.value.JdbcValueContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract SQL executor that provides common SQL execution logic.
 * Uses ValueProcessor for database-specific type conversions.
 *
 *
 * <p><b>Note:</b> This executor does NOT close the connection. The caller is
 * responsible for managing the connection lifecycle.
 *
 * @author Data-Agent Team
 */
public abstract class AbstractSqlExecutor implements CommandExecutor<SqlCommandRequest, SqlCommandResult> {

    private static final Logger log = LoggerFactory.getLogger(AbstractSqlExecutor.class);

    protected abstract Object getJdbcValue(JdbcValueContext context) throws SQLException;

    protected boolean getOriginalAutoCommit(Connection connection) {
        try {
            return connection.getAutoCommit();
        } catch (SQLException e) {
            log.warn("Failed to get original autoCommit: {}", e.getMessage());
            return true;
        }
    }

    @Override
    public SqlCommandResult executeCommand(final SqlCommandRequest command) {
        Connection connection = command.getConnection();
        SqlCommandResult result = initResult(command);
        boolean originalAutoCommit = getOriginalAutoCommit(connection);

        try {
            disableAutoCommitIfNeeded(connection, command);
            executeSqlStatement(connection, command, result);
            commitTransactionIfNeeded(connection, command);
            return result;

        } catch (SQLException e) {
            handleSqlException(connection, command, result, e);
            return result;

        } finally {
            restoreAutoCommit(connection, command, originalAutoCommit);
        }
    }

    /**
     * Create initial result object with basic information
     */
    private SqlCommandResult initResult(SqlCommandRequest command) {
        SqlCommandResult result = new SqlCommandResult();
        result.setSuccess(true);
        result.setOriginalSql(command.getOriginalSql());
        result.setExecutedSql(command.getExecuteSql());
        return result;
    }

    /**
     * Disable autoCommit if transaction is needed.
     * Subclasses can override this method to customize transaction handling
     * for databases that don't support transactions or have special requirements.
     *
     * @param connection the database connection
     * @param command    the SQL command request
     * @throws SQLException if unable to set autoCommit
     */
    protected void disableAutoCommitIfNeeded(Connection connection, SqlCommandRequest command) throws SQLException {
        if (command.isNeedTransaction()) {
            connection.setAutoCommit(false);
        }
    }

    /**
     * Execute SQL statement and populate result
     */
    private void executeSqlStatement(Connection connection, SqlCommandRequest command, SqlCommandResult result)
            throws SQLException {
        try (Statement statement = connection.createStatement()) {
            String sql = command.getExecuteSql();
            long start = System.currentTimeMillis();
            result.setStartTime(start);
            boolean hasResultSet = statement.execute(sql);
            long execEnd = System.currentTimeMillis();
            result.setExecutionMs(execEnd - start);
            List<SqlCommandSubResult> results = new ArrayList<>();
            while (true) {
                if (hasResultSet) {
                    SqlCommandSubResult sub = new SqlCommandSubResult();
                    sub.setQuery(true);
                    sub.setExecutionMs(result.getExecutionMs());
                    processQueryResult(statement, result, sub);
                    results.add(sub);
                } else {
                    int updateCount = statement.getUpdateCount();
                    if (updateCount == -1) {
                        break;
                    }
                    SqlCommandSubResult sub = new SqlCommandSubResult();
                    sub.setQuery(false);
                    sub.setExecutionMs(result.getExecutionMs());
                    sub.setFetchingMs(0L);
                    processDmlResult(updateCount, sub);
                    results.add(sub);
                }
                hasResultSet = statement.getMoreResults();
            }
            result.setResults(results);
            if (!results.isEmpty()) {
                applyFirstResult(result, results.get(0));
            }
            addWarnings(statement.getWarnings(), result, null);
            long end = System.currentTimeMillis();
            result.setEndTime(end);
            result.setExecutionTime(end - start);
        }
    }

    /**
     * Process DML operation result
     */
    private void processDmlResult(int updateCount, SqlCommandSubResult sub) {
        sub.setAffectedRows(updateCount);
    }

    private void addWarnings(SQLWarning warning, SqlCommandResult result, SqlCommandSubResult sub) {
        SQLWarning current = warning;
        while (current != null) {
            addMessage(result, sub, new SqlMessageInfo(
                    SqlMessageLevel.WARN,
                    String.valueOf(current.getErrorCode()),
                    current.getSQLState(),
                    current.getMessage(),
                    current.toString()
            ));
            current = current.getNextWarning();
        }
    }

    private void addMessage(SqlCommandResult result, SqlCommandSubResult sub, SqlMessageInfo message) {
        if (result.getMessages() == null) {
            result.setMessages(new ArrayList<>());
        }
        result.getMessages().add(message);
        if (sub != null) {
            if (sub.getMessages() == null) {
                sub.setMessages(new ArrayList<>());
            }
            sub.getMessages().add(message);
        }
    }

    /**
     * Commit transaction if needed.
     * Subclasses can override this method to customize commit behavior
     * for databases that don't support transactions.
     *
     * @param connection the database connection
     * @param command    the SQL command request
     * @throws SQLException if unable to commit
     */
    protected void commitTransactionIfNeeded(Connection connection, SqlCommandRequest command) throws SQLException {
        if (command.isNeedTransaction()) {
            connection.commit();
        }
    }

    /**
     * Handle SQL exception and rollback transaction if needed
     */
    private void handleSqlException(Connection connection, SqlCommandRequest command, SqlCommandResult result,
                                    SQLException e) {
        rollbackTransactionIfNeeded(connection, command, e);
        result.setSuccess(false);
        String message = e.getMessage() != null ? e.getMessage() : "Unknown error";
        result.setErrorMessage(e.getClass().getSimpleName() + ": " + message);
        result.setErrorCode(e.getErrorCode());
        result.setSqlState(e.getSQLState());
        result.setErrorDetail(e.toString());
        addMessage(result, null, new SqlMessageInfo(
                SqlMessageLevel.ERROR,
                String.valueOf(e.getErrorCode()),
                e.getSQLState(),
                message,
                e.toString()
        ));
    }

    /**
     * Rollback transaction if needed.
     * Subclasses can override this method to customize rollback behavior
     * for databases that don't support transactions.
     *
     * @param connection the database connection
     * @param command    the SQL command request
     * @param e          the original SQLException
     */
    protected void rollbackTransactionIfNeeded(Connection connection, SqlCommandRequest command, SQLException e) {
        if (command.isNeedTransaction()) {
            try {
                if (!connection.isClosed()) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                e.addSuppressed(rollbackEx);
            }
        }
    }

    /**
     * Restore original autoCommit state.
     * Subclasses can override this method to customize autoCommit restoration
     * for databases that don't support transactions.
     *
     * @param connection         the database connection
     * @param command            the SQL command request
     * @param originalAutoCommit the original autoCommit state to restore
     */
    protected void restoreAutoCommit(Connection connection, SqlCommandRequest command, boolean originalAutoCommit) {
        if (command.isNeedTransaction()) {
            try {
                if (!connection.isClosed()) {
                    connection.setAutoCommit(originalAutoCommit);
                }
            } catch (SQLException e) {
                log.warn("Failed to restore autoCommit: {}", e.getMessage());
            }
        }
    }

    /**
     * Process query result
     *
     * @param statement SQL statement
     * @param result    result object
     * @throws SQLException SQL exception
     */
    private void processQueryResult(Statement statement, SqlCommandResult result, SqlCommandSubResult sub) throws SQLException {
        List<String> headers = new ArrayList<>();
        List<SqlColumnInfo> columns = new ArrayList<>();

        try (ResultSet resultSet = statement.getResultSet()) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Get column names (use 1-based index consistently)
            for (int i = 1; i <= columnCount; i++) {
                String name = metaData.getColumnName(i);
                String label = metaData.getColumnLabel(i);
                String header = (label != null && !label.isBlank()) ? label : name;
                headers.add(header);
                SqlColumnInfo columnInfo = new SqlColumnInfo(
                        name,
                        label,
                        metaData.getColumnTypeName(i),
                        metaData.getColumnType(i),
                        metaData.getPrecision(i),
                        metaData.getScale(i),
                        metaData.isNullable(i) == ResultSetMetaData.columnNullable,
                        metaData.getTableName(i)
                );
                columns.add(columnInfo);
            }

            // Get data rows
            long fetchStart = System.currentTimeMillis();
            List<List<Object>> rows = new ArrayList<>();
            while (resultSet.next()) {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    // Build context from metadata using factory
                    JdbcValueContext context = JdbcValueContextFactory.fromMetaData(resultSet, metaData, i);
                    Object value = getJdbcValue(context);
                    row.add(value);
                }
                rows.add(row);
            }
            long fetchEnd = System.currentTimeMillis();

            sub.setHeaders(headers);
            sub.setRows(rows);
            sub.setColumns(columns);
            sub.setFetchRows(rows.size());
            sub.setFetchingMs(fetchEnd - fetchStart);
            addWarnings(resultSet.getWarnings(), result, sub);
        }
    }

    private void applyFirstResult(SqlCommandResult result, SqlCommandSubResult first) {
        result.setQuery(first.isQuery());
        result.setHeaders(first.getHeaders());
        result.setRows(first.getRows());
        result.setColumns(first.getColumns());
        result.setAffectedRows(first.getAffectedRows());
        result.setFetchRows(first.getFetchRows());
        result.setTruncated(first.getTruncated());
        result.setLimitApplied(first.getLimitApplied());
        result.setFetchingMs(first.getFetchingMs());
    }
}
