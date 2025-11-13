package edu.zsc.ai.plugin.model.command.sql;

import edu.zsc.ai.plugin.capability.CommandExecutor;
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
 * <p><b>Note:</b> This executor does NOT close the connection. The caller is
 * responsible for managing the connection lifecycle.
 *
 * @author Data-Agent Team
 */
public abstract class AbstractSqlExecutor implements CommandExecutor<SqlCommandRequest, SqlCommandResult> {

    /**
     * Extract value from ResultSet at the specified column.
     *
     * @param resultSet the ResultSet to extract from
     * @param columnIndex the column index (1-based)
     * @param sqlType the SQL type from java.sql.Types
     * @return the extracted value
     * @throws SQLException if value extraction fails
     */
    protected abstract Object extractValue(ResultSet resultSet, int columnIndex, int sqlType) throws SQLException;

    @Override
    public SqlCommandResult executeCommand(SqlCommandRequest command) {
        Connection connection = command.getConnection();
        SqlCommandResult result = new SqlCommandResult();
        result.setSuccess(true);
        result.setOriginalSql(command.getOriginalSql());
        result.setExecutedSql(command.getExecuteSql());

        boolean originalAutoCommit = true;

        try {
            // 保存原始 autoCommit 状态
            originalAutoCommit = connection.getAutoCommit();

            if (command.isNeedTransaction()) {
                connection.setAutoCommit(false);
            }

            try (Statement statement = connection.createStatement()) {
                String sql = command.getExecuteSql();
                long start = System.currentTimeMillis();
                boolean isQuery = statement.execute(sql);
                result.setExecutionTime(System.currentTimeMillis() - start);

                result.setQuery(isQuery);
                if (!isQuery) {
                    // DML 操作
                    int updateCount = statement.getUpdateCount();
                    result.setAffectedRows(updateCount);
                } else {
                    // 查询操作
                    processQueryResult(statement, result);
                }

                // 提交事务
                if (command.isNeedTransaction()) {
                    connection.commit();
                }
            }

            return result;

        } catch (SQLException e) {
            // 回滚事务
            if (command.isNeedTransaction()) {
                try {
                    if (!connection.isClosed()) {
                        connection.rollback();
                    }
                } catch (SQLException rollbackEx) {
                    // 记录回滚失败,但不影响原始异常
                    e.addSuppressed(rollbackEx);
                }
            }

            result.setSuccess(false);
            result.setErrorMessage(
                    e.getClass().getSimpleName() + ": " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
            return result;

        } finally {
            // 恢复原始 autoCommit 状态
            if (command.isNeedTransaction()) {
                try {
                    if (!connection.isClosed()) {
                        connection.setAutoCommit(originalAutoCommit);
                    }
                } catch (SQLException e) {
                    // 记录但不抛出异常
                    System.err.println("Failed to restore autoCommit: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 处理查询结果
     *
     * @param statement SQL 语句
     * @param result 结果对象
     * @throws SQLException SQL 异常
     */
    private void processQueryResult(Statement statement, SqlCommandResult result) throws SQLException {
        List<String> headers = new ArrayList<>();

        try (ResultSet resultSet = statement.getResultSet()) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 获取列名 (统一使用 1-based 索引)
            for (int i = 1; i <= columnCount; i++) {
                String header = metaData.getColumnName(i);
                headers.add(header);
            }

            // 获取数据行
            List<List<Object>> rows = new ArrayList<>();
            while (resultSet.next()) {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnTypeName = metaData.getColumnTypeName(i);
                    int sqlType = metaData.getColumnType(i);
                    Object value = extractValue(resultSet, i, sqlType);
                    row.add(value);
                }
                rows.add(row);
            }

            result.setHeaders(headers);
            result.setRows(rows);
        }
    }
}
