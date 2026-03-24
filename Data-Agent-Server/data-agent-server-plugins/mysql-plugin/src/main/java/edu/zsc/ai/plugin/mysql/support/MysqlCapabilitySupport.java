package edu.zsc.ai.plugin.mysql.support;

import edu.zsc.ai.plugin.capability.MysqlIdentifierEscaper;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.model.metadata.ParameterInfo;
import edu.zsc.ai.plugin.mysql.constant.MySqlTemplate;
import edu.zsc.ai.plugin.mysql.constant.MysqlRoutineConstants;
import edu.zsc.ai.plugin.mysql.executor.MySQLSqlExecutor;
import edu.zsc.ai.plugin.mysql.util.MysqlIdentifierBuilder;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class MysqlCapabilitySupport {

    private final MySQLSqlExecutor sqlExecutor;

    public MysqlCapabilitySupport(MySQLSqlExecutor sqlExecutor) {
        this.sqlExecutor = Objects.requireNonNull(sqlExecutor, "sqlExecutor");
    }

    public String resolveDatabase(String catalog, String schema) {
        return StringUtils.isNotBlank(catalog) ? catalog : schema;
    }

    public SqlCommandResult execute(Connection connection, String database, String sql) {
        return sqlExecutor.executeCommand(
                SqlCommandRequest.ofWithoutTransaction(connection, sql, sql, database, null));
    }

    public long countObjectsByName(Connection connection, String database, String baseSql, String namePattern, String nameClause) {
        if (connection == null || StringUtils.isBlank(database)) {
            return 0;
        }

        boolean hasNameFilter = StringUtils.isNotBlank(namePattern) && !"%".equals(namePattern);
        String sql = hasNameFilter ? baseSql + nameClause : baseSql;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            statement.setString(parameterIndex++, database);
            if (hasNameFilter) {
                statement.setString(parameterIndex, namePattern);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("total");
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count MySQL objects: " + e.getMessage(), e);
        }
    }

    public String getObjectDdl(Connection connection, String catalog, String objectName,
                               String sqlTemplate, String columnName, String objectType) {
        if (connection == null || StringUtils.isBlank(objectName)) {
            return "";
        }

        String fullName = MysqlIdentifierBuilder.buildFullIdentifier(catalog, objectName);
        String sql = String.format(sqlTemplate, fullName);

        SqlCommandResult result = execute(connection, catalog, sql);
        if (!result.isSuccess()) {
            throw new RuntimeException(String.format("Failed to get %s DDL: %s", objectType, result.getErrorMessage()));
        }

        if (result.getRows() == null || result.getRows().isEmpty()) {
            throw new RuntimeException(String.format("Failed to get %s DDL: No result returned", objectType));
        }

        List<Object> firstRow = result.getRows().get(0);
        Object ddl = result.getValueByColumnName(firstRow, columnName);
        if (ddl == null) {
            throw new RuntimeException(String.format(
                    "Failed to get %s DDL: Column '%s' not found in result",
                    objectType,
                    columnName
            ));
        }
        return ddl.toString();
    }

    public void dropObject(Connection connection, String catalog, String objectName,
                           String sqlTemplate, String objectType, boolean useFullIdentifier) {
        if (connection == null || StringUtils.isBlank(objectName)) {
            throw new IllegalArgumentException(
                    String.format("Connection and %s name must not be null or empty", objectType));
        }

        String fullName = useFullIdentifier
                ? MysqlIdentifierBuilder.buildFullIdentifier(catalog, objectName)
                : MysqlIdentifierEscaper.getInstance().quoteIdentifier(objectName);
        String sql = String.format(sqlTemplate, fullName);

        SqlCommandResult result = execute(connection, catalog, sql);
        if (!result.isSuccess()) {
            throw new RuntimeException(String.format("Failed to delete %s: %s", objectType, result.getErrorMessage()));
        }
    }

    public SqlCommandResult getTableLikeData(Connection connection, String catalog, String objectName, int offset, int pageSize) {
        requireConnectionAndName(connection, objectName);

        String fullObjectName = MysqlIdentifierBuilder.buildFullIdentifier(catalog, objectName);
        String sql = String.format(MySqlTemplate.SQL_SELECT_TABLE_DATA, fullObjectName, pageSize, offset);

        SqlCommandResult result = execute(connection, catalog, sql);
        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to get table data: " + result.getErrorMessage());
        }
        return result;
    }

    public SqlCommandResult getTableLikeData(Connection connection, String catalog, String objectName,
                                             int offset, int pageSize, String whereClause,
                                             String orderByColumn, String orderByDirection) {
        requireConnectionAndName(connection, objectName);

        String fullObjectName = MysqlIdentifierBuilder.buildFullIdentifier(catalog, objectName);
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(fullObjectName);

        if (StringUtils.isNotBlank(whereClause)) {
            sql.append(" WHERE ").append(whereClause);
        }
        if (StringUtils.isNotBlank(orderByColumn)) {
            String direction = "desc".equalsIgnoreCase(orderByDirection) ? "DESC" : "ASC";
            String quotedColumn = MysqlIdentifierEscaper.getInstance().quoteIdentifier(orderByColumn.trim());
            sql.append(" ORDER BY ").append(quotedColumn).append(" ").append(direction);
        }
        sql.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(offset);

        String sqlText = sql.toString();
        SqlCommandResult result = execute(connection, catalog, sqlText);
        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to get table data: " + result.getErrorMessage());
        }
        return result;
    }

    public long getTableLikeDataCount(Connection connection, String catalog, String objectName) {
        return getTableLikeDataCount(connection, catalog, objectName, null);
    }

    public long getTableLikeDataCount(Connection connection, String catalog, String objectName, String whereClause) {
        requireConnectionAndName(connection, objectName);

        String fullObjectName = MysqlIdentifierBuilder.buildFullIdentifier(catalog, objectName);
        String sql = StringUtils.isNotBlank(whereClause)
                ? "SELECT COUNT(*) AS total FROM " + fullObjectName + " WHERE " + whereClause
                : String.format(MySqlTemplate.SQL_COUNT_TABLE_DATA, fullObjectName);

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getLong("total");
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get table data count: " + e.getMessage(), e);
        }
    }

    public List<ParamRow> fetchParameters(Connection connection, String database, Set<String> specificNames) {
        if (specificNames == null || specificNames.isEmpty()) {
            return List.of();
        }

        StringBuilder inClause = new StringBuilder();
        for (String specificName : specificNames) {
            if (!inClause.isEmpty()) {
                inClause.append(',');
            }
            String escapedSpecificName = MysqlIdentifierEscaper.getInstance().escapeStringLiteral(specificName);
            inClause.append('\'').append(escapedSpecificName).append('\'');
        }

        String escapedDatabase = MysqlIdentifierEscaper.getInstance().escapeStringLiteral(database);
        String sql = String.format(MySqlTemplate.SQL_FETCH_PARAMETERS, escapedDatabase, inClause);

        SqlCommandResult result = execute(connection, database, sql);
        if (!result.isSuccess()) {
            return List.of();
        }

        List<ParamRow> rows = new ArrayList<>();
        if (result.getRows() == null) {
            return rows;
        }

        for (List<Object> row : result.getRows()) {
            Object specificName = result.getValueByColumnName(row, MysqlRoutineConstants.SPECIFIC_NAME);
            Object parameterName = result.getValueByColumnName(row, MysqlRoutineConstants.PARAMETER_NAME);
            Object dataType = result.getValueByColumnName(row, MysqlRoutineConstants.DTD_IDENTIFIER);
            Object position = result.getValueByColumnName(row, MysqlRoutineConstants.ORDINAL_POSITION);

            rows.add(new ParamRow(
                    specificName != null ? specificName.toString() : "",
                    parameterName != null ? parameterName.toString() : "",
                    dataType != null ? dataType.toString().trim() : "",
                    position != null ? ((Number) position).intValue() : 0
            ));
        }
        return rows;
    }

    public Map<String, List<ParameterInfo>> groupParametersByRoutine(List<ParamRow> rows) {
        Map<String, List<ParamRow>> rowsBySpecificName = new LinkedHashMap<>();
        for (ParamRow row : rows) {
            rowsBySpecificName.computeIfAbsent(row.specificName(), ignored -> new ArrayList<>()).add(row);
        }

        Map<String, List<ParameterInfo>> parametersByRoutine = new LinkedHashMap<>();
        for (Map.Entry<String, List<ParamRow>> entry : rowsBySpecificName.entrySet()) {
            List<ParameterInfo> parameters = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(ParamRow::ordinalPosition))
                    .map(row -> new ParameterInfo(row.parameterName(), row.dataType()))
                    .toList();
            parametersByRoutine.put(entry.getKey(), parameters);
        }
        return parametersByRoutine;
    }

    private void requireConnectionAndName(Connection connection, String objectName) {
        if (connection == null || StringUtils.isBlank(objectName)) {
            throw new IllegalArgumentException("Connection and object name must not be null or empty");
        }
    }

    public record ParamRow(String specificName, String parameterName, String dataType, int ordinalPosition) {
    }
}
