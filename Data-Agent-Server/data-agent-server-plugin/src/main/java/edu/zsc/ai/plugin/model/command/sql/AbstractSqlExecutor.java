package edu.zsc.ai.plugin.model.command.sql;

import edu.zsc.ai.plugin.capability.CommandExecutor;
import edu.zsc.ai.plugin.value.ValueProcessor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract SQL executor that provides common SQL execution logic.
 * Uses ValueProcessor for database-specific type conversions.
 *
 * <p>Inspired by Chat2DB's design, this class separates SQL execution logic
 * from type handling logic, making it easier to extend and test.
 *
 * @author Data-Agent Team
 */
public abstract class AbstractSqlExecutor implements CommandExecutor<SqlCommandRequest, SqlCommandResult> {

    /**
     * Get the value processor for handling database-specific type conversions.
     *
     * @return the value processor instance
     */
    protected abstract ValueProcessor getValueProcessor();

    @Override
    public SqlCommandResult executeCommand(SqlCommandRequest command) {
        Connection connection = command.getConnection();
        SqlCommandResult result = new SqlCommandResult();
        result.setSuccess(true);
        result.setOriginalSql(command.getOriginalSql());
        result.setExecutedSql(command.getExecuteSql());
        try (Statement statement = connection.createStatement()) {
            if (command.isNeedTransaction()) {
                connection.setAutoCommit(false);
            }
            String sql = command.getExecuteSql();
            long start = System.currentTimeMillis();
            boolean execute = statement.execute(sql);
            result.setExecutionTime(System.currentTimeMillis() - start);

            if (command.isNeedTransaction()) {
                connection.commit();
            }

            result.setQuery(execute);
            if (!execute) {
                // not query
                int updateCount = statement.getUpdateCount();
                result.setAffectedRows(updateCount);
            } else {
                // query
                List<String> headers = new ArrayList<>();
                try (ResultSet resultSet = statement.getResultSet()) {
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    for (int i = 0; i < columnCount; i++) {
                        String header = metaData.getColumnName(i + 1);
                        headers.add(header);
                    }
                    List<List<Object>> rows = new ArrayList<>();
                    while (resultSet.next()) {
                        List<Object> row = new ArrayList<>();
                        for (int i = 1; i <= columnCount; i++) {
                            // Use ValueProcessor for database-specific type conversion
                            String columnTypeName = metaData.getColumnTypeName(i);
                            int sqlType = metaData.getColumnType(i);
                            Object value = getValueProcessor().getJdbcValue(
                                    resultSet, i, sqlType, columnTypeName);
                            row.add(value);
                        }
                        rows.add(row);
                    }
                    result.setHeaders(headers);
                    result.setRows(rows);
                }
            }
            return result;
        } catch (Exception e) {
            try {
                if (command.isNeedTransaction() && !connection.isClosed()) {
                    connection.rollback();
                }
                result.setSuccess(false);
                result.setErrorMessage(e.getMessage());
                return result;
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
