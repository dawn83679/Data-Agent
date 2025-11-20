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
            boolean isQuery = statement.execute(sql);
            result.setExecutionTime(System.currentTimeMillis() - start);

            result.setQuery(isQuery);
            if (isQuery) {
                processQueryResult(statement, result);
            } else {
                processDmlResult(statement, result);
            }
        }
    }

    /**
     * Process DML operation result
     */
    private void processDmlResult(Statement statement, SqlCommandResult result) throws SQLException {
        int updateCount = statement.getUpdateCount();
        result.setAffectedRows(updateCount);
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
        result.setErrorMessage(
                e.getClass().getSimpleName() + ": " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
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
    private void processQueryResult(Statement statement, SqlCommandResult result) throws SQLException {
        List<String> headers = new ArrayList<>();

        try (ResultSet resultSet = statement.getResultSet()) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Get column names (use 1-based index consistently)
            for (int i = 1; i <= columnCount; i++) {
                String header = metaData.getColumnName(i);
                headers.add(header);
            }

            // Get data rows
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

            result.setHeaders(headers);
            result.setRows(rows);
        }
    }
}
