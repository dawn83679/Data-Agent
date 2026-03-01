package edu.zsc.ai.plugin.mysql;

import edu.zsc.ai.plugin.base.AbstractDatabasePlugin;
import edu.zsc.ai.plugin.capability.*;
import edu.zsc.ai.plugin.sql.DefaultSqlSplitter;
import edu.zsc.ai.plugin.connection.ConnectionConfig;
import edu.zsc.ai.plugin.connection.JdbcConnectionBuilder;
import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;
import edu.zsc.ai.plugin.constant.IsNullableEnum;
import edu.zsc.ai.plugin.driver.DriverLoader;
import edu.zsc.ai.plugin.driver.MavenCoordinates;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.model.metadata.*;
import edu.zsc.ai.plugin.mysql.connection.MysqlJdbcConnectionBuilder;
import edu.zsc.ai.plugin.mysql.constant.*;
import edu.zsc.ai.plugin.mysql.executor.MySQLSqlExecutor;
import edu.zsc.ai.plugin.mysql.util.MysqlIdentifierBuilder;
import edu.zsc.ai.plugin.mysql.value.MySQLDataTypeEnum;
import org.apache.commons.lang3.StringUtils;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public abstract class DefaultMysqlPlugin extends AbstractDatabasePlugin
        implements ConnectionProvider, CommandExecutor<SqlCommandRequest, SqlCommandResult>, DatabaseProvider,
        SchemaProvider, TableProvider, ViewProvider, ColumnProvider, IndexProvider,
        FunctionProvider, ProcedureProvider, TriggerProvider, SqlSplitter {

    private static final Logger logger = Logger.getLogger(DefaultMysqlPlugin.class.getName());

    private final JdbcConnectionBuilder connectionBuilder = new MysqlJdbcConnectionBuilder();

    private final MySQLSqlExecutor sqlExecutor = new MySQLSqlExecutor();

    @Override
    public boolean supportSchema() {
        return false;
    }

    protected abstract String getDriverClassName();

    protected String getJdbcUrlTemplate() {
        return "jdbc:mysql://%s:%d/%s";
    }

    protected int getDefaultPort() {
        return 3306;
    }

    @Override
    public Connection connect(ConnectionConfig config) {
        try {
            DriverLoader.loadDriver(config, getDriverClassName());

            String jdbcUrl = connectionBuilder.buildUrl(config, getJdbcUrlTemplate(), getDefaultPort());

            Properties properties = connectionBuilder.buildProperties(config);

            Connection connection = DriverManager.getConnection(jdbcUrl, properties);

            logger.info(String.format("Successfully connected to MySQL database at %s:%d/%s",
                    config.getHost(),
                    config.getPort() != null ? config.getPort() : getDefaultPort(),
                    config.getDatabase() != null ? config.getDatabase() : ""));

            return connection;

        } catch (SQLException e) {
            String errorMsg = String.format("Failed to connect to MySQL database at %s:%d/%s: %s",
                    config.getHost(),
                    config.getPort() != null ? config.getPort() : getDefaultPort(),
                    config.getDatabase() != null ? config.getDatabase() : "",
                    e.getMessage());
            logger.severe(errorMsg);
            throw new RuntimeException(errorMsg, e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Unexpected error while connecting to MySQL database: %s", e.getMessage());
            logger.severe(errorMsg);
            throw new RuntimeException(errorMsg, e);
        }
    }

    @Override
    public boolean testConnection(ConnectionConfig config) {
        try {
            Connection connection = connect(config);
            if (connection != null && !connection.isClosed()) {
                closeConnection(connection);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.warning(String.format("Connection test failed: %s", e.getMessage()));
            return false;
        }
    }

    @Override
    public void closeConnection(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to close database connection: " + e.getMessage(), e);
        }
    }

    @Override
    public SqlCommandResult executeCommand(SqlCommandRequest command) {
        return sqlExecutor.executeCommand(command);
    }

    @Override
    public java.util.List<String> split(String sql) {
        return DefaultSqlSplitter.INSTANCE.split(sql);
    }

    @Override
    public MavenCoordinates getDriverMavenCoordinates(String driverVersion) {
        if (driverVersion == null || driverVersion.isEmpty()
                || driverVersion.startsWith("8.") || driverVersion.startsWith("9.")) {
            String version = (driverVersion != null && !driverVersion.isEmpty()) ? driverVersion : "8.0.33";
            return new MavenCoordinates(
                    "com.mysql",
                    "mysql-connector-j",
                    version
            );
        }

        char firstChar = driverVersion.charAt(0);
        if (firstChar >= '2' && firstChar <= '7') {
            return new MavenCoordinates(
                    "mysql",
                    "mysql-connector-java",
                    driverVersion
            );
        }

        throw new IllegalArgumentException(
                String.format("Unsupported MySQL driver version: %s. Supported versions: 2.x-7.x, 8.x, 9.x", driverVersion));
    }

    @Override
    public List<ColumnMetadata> getColumns(Connection connection, String catalog, String schema, String tableOrViewName) {
        if (connection == null || StringUtils.isBlank(tableOrViewName)) {
            return List.of();
        }
        String db = StringUtils.isNotBlank(catalog) ? catalog : schema;
        if (StringUtils.isBlank(db)) {
            return List.of();
        }
        String escapedDb = MysqlIdentifierEscaper.getInstance().escapeStringLiteral(db);
        String escapedTable = MysqlIdentifierEscaper.getInstance().escapeStringLiteral(tableOrViewName);
        String sql = String.format(MysqlSqlConstants.SQL_LIST_COLUMNS, escapedDb, escapedTable);

        SqlCommandResult result = sqlExecutor.executeCommand(
                SqlCommandRequest.ofWithoutTransaction(connection, sql, sql, db, null));
        if (!result.isSuccess()) {
            logger.severe("Failed to list columns for " + tableOrViewName + ": " + result.getErrorMessage());
            throw new RuntimeException("Failed to list columns: " + result.getErrorMessage());
        }

        List<ColumnMetadata> list = new ArrayList<>();
        if (result.getRows() != null) {
            for (List<Object> row : result.getRows()) {
                Object nameObj = result.getValueByColumnName(row, MysqlColumnConstants.COLUMN_NAME);
                Object posObj = result.getValueByColumnName(row, MysqlColumnConstants.ORDINAL_POSITION);
                Object defObj = result.getValueByColumnName(row, MysqlColumnConstants.COLUMN_DEFAULT);
                Object nullableObj = result.getValueByColumnName(row, MysqlColumnConstants.IS_NULLABLE);
                Object dataTypeObj = result.getValueByColumnName(row, MysqlColumnConstants.DATA_TYPE);
                Object columnTypeObj = result.getValueByColumnName(row, MysqlColumnConstants.COLUMN_TYPE);
                Object columnKeyObj = result.getValueByColumnName(row, MysqlColumnConstants.COLUMN_KEY);
                Object extraObj = result.getValueByColumnName(row, MysqlColumnConstants.EXTRA);
                Object commentObj = result.getValueByColumnName(row, MysqlColumnConstants.COLUMN_COMMENT);
                Object charLenObj = result.getValueByColumnName(row, MysqlColumnConstants.CHARACTER_MAXIMUM_LENGTH);
                Object numPrecObj = result.getValueByColumnName(row, MysqlColumnConstants.NUMERIC_PRECISION);
                Object numScaleObj = result.getValueByColumnName(row, MysqlColumnConstants.NUMERIC_SCALE);

                String name = nameObj != null ? nameObj.toString() : "";
                if (name.isEmpty()) continue;

                int ordinalPosition = posObj != null ? ((Number) posObj).intValue() : 0;
                String defaultValue = defObj != null ? defObj.toString() : null;
                boolean nullable = IsNullableEnum.isNullable(nullableObj != null ? nullableObj.toString() : null);
                String dataTypeStr = dataTypeObj != null ? dataTypeObj.toString() : "";
                String columnType = columnTypeObj != null ? columnTypeObj.toString() : "";
                String columnKey = columnKeyObj != null ? columnKeyObj.toString() : "";
                String extra = extraObj != null ? extraObj.toString() : "";
                String remarks = commentObj != null ? commentObj.toString() : "";
                int columnSize = charLenObj != null ? ((Number) charLenObj).intValue() : 0;
                if (columnSize == 0 && numPrecObj != null) {
                    columnSize = ((Number) numPrecObj).intValue();
                }
                int decimalDigits = numScaleObj != null ? ((Number) numScaleObj).intValue() : 0;

                boolean isPrimaryKeyPart = MysqlColumnConstants.COLUMN_KEY_PRI.equals(columnKey);
                boolean isAutoIncrement = extra.toLowerCase().contains(MysqlColumnConstants.EXTRA_AUTO_INCREMENT);
                boolean isUnsigned = columnType.toLowerCase().contains("unsigned");

                int javaSqlType = MySQLDataTypeEnum.toSqlType(dataTypeStr);
                list.add(new ColumnMetadata(
                        name,
                        javaSqlType,
                        dataTypeStr,
                        columnSize,
                        decimalDigits,
                        nullable,
                        ordinalPosition,
                        remarks,
                        isPrimaryKeyPart,
                        isAutoIncrement,
                        isUnsigned,
                        defaultValue
                ));
            }
        }
        list.sort(Comparator.comparingInt(ColumnMetadata::ordinalPosition));
        return list;
    }

    @Override
    public String getTableDdl(Connection connection, String catalog, String schema, String tableName) {
        return getObjectDdl(connection, catalog, tableName,
                MysqlSqlConstants.SQL_SHOW_CREATE_TABLE,
                MysqlShowColumnConstants.CREATE_TABLE,
                DatabaseObjectTypeEnum.TABLE.getValue());
    }

    @Override
    public String getViewDdl(Connection connection, String catalog, String schema, String viewName) {
        return getObjectDdl(connection, catalog, viewName,
                MysqlSqlConstants.SQL_SHOW_CREATE_VIEW,
                MysqlShowColumnConstants.CREATE_VIEW,
                DatabaseObjectTypeEnum.VIEW.getValue());
    }

    @Override
    public String getFunctionDdl(Connection connection, String catalog, String schema, String functionName) {
        return getObjectDdl(connection, catalog, functionName,
                MysqlSqlConstants.SQL_SHOW_CREATE_FUNCTION,
                MysqlShowColumnConstants.CREATE_FUNCTION,
                DatabaseObjectTypeEnum.FUNCTION.getValue());
    }

    @Override
    public String getProcedureDdl(Connection connection, String catalog, String schema, String procedureName) {
        return getObjectDdl(connection, catalog, procedureName,
                MysqlSqlConstants.SQL_SHOW_CREATE_PROCEDURE,
                MysqlShowColumnConstants.CREATE_PROCEDURE,
                DatabaseObjectTypeEnum.PROCEDURE.getValue());
    }

    @Override
    public String getTriggerDdl(Connection connection, String catalog, String schema, String triggerName) {
        return getObjectDdl(connection, catalog, triggerName,
                MysqlSqlConstants.SQL_SHOW_CREATE_TRIGGER,
                MysqlShowColumnConstants.SQL_ORIGINAL_STATEMENT,
                DatabaseObjectTypeEnum.TRIGGER.getValue());
    }

    @Override
    public SqlCommandResult getTableData(Connection connection, String catalog, String schema, String tableName, int offset, int pageSize) {
        return getTableData(connection, catalog, schema, tableName, offset, pageSize, null, null);
    }

    @Override
    public SqlCommandResult getTableData(Connection connection, String catalog, String schema, String tableName,
                                         int offset, int pageSize, String whereClause, String orderBy) {
        if (connection == null || StringUtils.isBlank(tableName)) {
            throw new IllegalArgumentException("Connection and table name must not be null or empty");
        }

        String fullTableName = MysqlIdentifierBuilder.buildFullIdentifier(catalog, tableName);
        String sql = buildSelectSql(fullTableName, whereClause, orderBy, pageSize, offset);

        SqlCommandResult result = sqlExecutor.executeCommand(
                SqlCommandRequest.ofWithoutTransaction(connection, sql, sql, catalog, null));

        if (!result.isSuccess()) {
            logger.severe(String.format("Failed to get table data for %s: %s",
                    fullTableName, result.getErrorMessage()));
            throw new RuntimeException("Failed to get table data: " + result.getErrorMessage());
        }

        return result;
    }

    @Override
    public long getTableDataCount(Connection connection, String catalog, String schema, String tableName) {
        return getTableDataCount(connection, catalog, schema, tableName, null);
    }

    @Override
    public long getTableDataCount(Connection connection, String catalog, String schema, String tableName,
                                  String whereClause) {
        if (connection == null || StringUtils.isBlank(tableName)) {
            throw new IllegalArgumentException("Connection and table name must not be null or empty");
        }

        String fullTableName = MysqlIdentifierBuilder.buildFullIdentifier(catalog, tableName);
        String sql = buildCountSql(fullTableName, whereClause);

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("total");
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get table data count: " + e.getMessage(), e);
        }
    }

    @Override
    public SqlCommandResult getViewData(Connection connection, String catalog, String schema, String viewName, int offset, int pageSize) {
        return getTableData(connection, catalog, schema, viewName, offset, pageSize);
    }

    @Override
    public SqlCommandResult getViewData(Connection connection, String catalog, String schema, String viewName,
                                        int offset, int pageSize, String whereClause, String orderBy) {
        return getTableData(connection, catalog, schema, viewName, offset, pageSize, whereClause, orderBy);
    }

    @Override
    public long getViewDataCount(Connection connection, String catalog, String schema, String viewName) {
        return getTableDataCount(connection, catalog, schema, viewName);
    }

    @Override
    public long getViewDataCount(Connection connection, String catalog, String schema, String viewName,
                                 String whereClause) {
        return getTableDataCount(connection, catalog, schema, viewName, whereClause);
    }

    private String buildSelectSql(String fullTableName, String whereClause, String orderBy, int pageSize, int offset) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ").append(fullTableName);
        if (StringUtils.isNotBlank(whereClause)) {
            sb.append(" WHERE ").append(whereClause);
        }
        if (StringUtils.isNotBlank(orderBy)) {
            sb.append(" ORDER BY ").append(orderBy);
        }
        sb.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(offset);
        return sb.toString();
    }

    private String buildCountSql(String fullTableName, String whereClause) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT COUNT(*) AS total FROM ").append(fullTableName);
        if (StringUtils.isNotBlank(whereClause)) {
            sb.append(" WHERE ").append(whereClause);
        }
        return sb.toString();
    }

    @Override
    public List<TriggerMetadata> getTriggers(Connection connection, String catalog, String schema, String tableName) {
        if (connection == null) {
            return List.of();
        }
        String db = StringUtils.isNotBlank(catalog) ? catalog : schema;
        if (StringUtils.isBlank(db)) {
            return List.of();
        }
        String escapedDb = MysqlIdentifierEscaper.getInstance().escapeStringLiteral(db);
        String sql = String.format(MysqlSqlConstants.SQL_LIST_TRIGGERS, escapedDb);
        if (StringUtils.isNotBlank(tableName)) {
            String escapedTable = MysqlIdentifierEscaper.getInstance().escapeStringLiteral(tableName);
            sql += MysqlSqlConstants.SQL_TRIGGER_FILTER_BY_TABLE + escapedTable + "'";
        }

        SqlCommandResult result = sqlExecutor.executeCommand(
                SqlCommandRequest.ofWithoutTransaction(connection, sql, sql, db, null));
        if (!result.isSuccess()) {
            logger.severe("Failed to list triggers: " + result.getErrorMessage());
            throw new RuntimeException("Failed to list triggers: " + result.getErrorMessage());
        }

        List<TriggerMetadata> list = new ArrayList<>();
        if (result.getRows() != null) {
            for (List<Object> row : result.getRows()) {
                Object nameObj = result.getValueByColumnName(row, MysqlTriggerConstants.TRIGGER_NAME);
                Object tableObj = result.getValueByColumnName(row, MysqlTriggerConstants.EVENT_OBJECT_TABLE);
                Object timingObj = result.getValueByColumnName(row, MysqlTriggerConstants.ACTION_TIMING);
                Object eventObj = result.getValueByColumnName(row, MysqlTriggerConstants.EVENT_MANIPULATION);
                String name = nameObj != null ? nameObj.toString() : "";
                String tbl = tableObj != null ? tableObj.toString() : "";
                String timing = timingObj != null ? timingObj.toString() : "";
                String event = eventObj != null ? eventObj.toString() : "";
                if (StringUtils.isNotBlank(name)) {
                    list.add(new TriggerMetadata(name, tbl, timing, event));
                }
            }
        }
        return list;
    }

    @Override
    public List<FunctionMetadata> getFunctions(Connection connection, String catalog, String schema) {
        if (connection == null) {
            return List.of();
        }
        String db = StringUtils.isNotBlank(catalog) ? catalog : schema;
        if (StringUtils.isBlank(db)) {
            return List.of();
        }
        String escapedDb = MysqlIdentifierEscaper.getInstance().escapeStringLiteral(db);
        String sql = String.format(MysqlSqlConstants.SQL_LIST_FUNCTIONS, escapedDb);

        SqlCommandResult result = sqlExecutor.executeCommand(
                SqlCommandRequest.ofWithoutTransaction(connection, sql, sql, db, null));

        if (!result.isSuccess()) {
            logger.severe("Failed to list functions: " + result.getErrorMessage());
            throw new RuntimeException("Failed to list functions: " + result.getErrorMessage());
        }

        Map<String, FunctionMetadata> functionsByName = new LinkedHashMap<>();
        if (result.getRows() != null) {
            for (List<Object> row : result.getRows()) {
                Object specNameObj = result.getValueByColumnName(row, MysqlRoutineConstants.SPECIFIC_NAME);
                Object nameObj = result.getValueByColumnName(row, MysqlRoutineConstants.ROUTINE_NAME);
                Object dtdObj = result.getValueByColumnName(row, MysqlRoutineConstants.DTD_IDENTIFIER);
                String specName = specNameObj != null ? specNameObj.toString() : "";
                String name = nameObj != null ? nameObj.toString() : "";
                String returnType = dtdObj != null ? dtdObj.toString().trim() : null;
                if (!name.isEmpty() && !functionsByName.containsKey(specName)) {
                    functionsByName.put(specName, new FunctionMetadata(name, null, returnType));
                }
            }
        }

        List<ParamRow> allParams = fetchParameters(connection, db, functionsByName.keySet());
        Map<String, List<ParameterInfo>> paramsByRoutine = groupParametersByRoutine(allParams);

        List<FunctionMetadata> list = new ArrayList<>();
        for (Map.Entry<String, FunctionMetadata> e : functionsByName.entrySet()) {
            FunctionMetadata fm = e.getValue();
            List<ParameterInfo> params = paramsByRoutine.getOrDefault(e.getKey(), List.of());
            list.add(new FunctionMetadata(fm.name(), params.isEmpty() ? null : params, fm.returnType()));
        }
        return list;
    }

    @Override
    public List<ProcedureMetadata> getProcedures(Connection connection, String catalog, String schema) {
        if (connection == null) {
            return List.of();
        }
        String db = StringUtils.isNotBlank(catalog) ? catalog : schema;
        if (StringUtils.isBlank(db)) {
            return List.of();
        }
        String escapedDb = MysqlIdentifierEscaper.getInstance().escapeStringLiteral(db);
        String sql = String.format(MysqlSqlConstants.SQL_LIST_PROCEDURES, escapedDb);

        SqlCommandResult result = sqlExecutor.executeCommand(
                SqlCommandRequest.ofWithoutTransaction(connection, sql, sql, db, null));

        if (!result.isSuccess()) {
            logger.severe("Failed to list procedures: " + result.getErrorMessage());
            throw new RuntimeException("Failed to list procedures: " + result.getErrorMessage());
        }

        Map<String, ProcedureMetadata> proceduresByName = new LinkedHashMap<>();
        if (result.getRows() != null) {
            for (List<Object> row : result.getRows()) {
                Object specNameObj = result.getValueByColumnName(row, MysqlRoutineConstants.SPECIFIC_NAME);
                Object nameObj = result.getValueByColumnName(row, MysqlRoutineConstants.ROUTINE_NAME);
                String specName = specNameObj != null ? specNameObj.toString() : "";
                String name = nameObj != null ? nameObj.toString() : "";
                if (StringUtils.isNotBlank(name) && !proceduresByName.containsKey(specName)) {
                    proceduresByName.put(specName, new ProcedureMetadata(name, null));
                }
            }
        }

        List<ParamRow> allParams = fetchParameters(connection, db, proceduresByName.keySet());
        Map<String, List<ParameterInfo>> paramsByRoutine = groupParametersByRoutine(allParams);

        List<ProcedureMetadata> list = new ArrayList<>();
        for (Map.Entry<String, ProcedureMetadata> e : proceduresByName.entrySet()) {
            ProcedureMetadata pm = e.getValue();
            List<ParameterInfo> params = paramsByRoutine.getOrDefault(e.getKey(), List.of());
            list.add(new ProcedureMetadata(pm.name(), params.isEmpty() ? null : params));
        }
        return list;
    }

    private List<ParamRow> fetchParameters(Connection connection, String db, java.util.Set<String> specificNames) {
        if (specificNames == null || specificNames.isEmpty()) {
            return List.of();
        }
        StringBuilder inClause = new StringBuilder();
        for (String sn : specificNames) {
            if (inClause.length() > 0) inClause.append(',');
            String escapedSn = MysqlIdentifierEscaper.getInstance().escapeStringLiteral(sn);
            inClause.append("'").append(escapedSn).append("'");
        }
        String escapedDb = MysqlIdentifierEscaper.getInstance().escapeStringLiteral(db);
        String sql = String.format(MysqlSqlConstants.SQL_FETCH_PARAMETERS, escapedDb, inClause);

        SqlCommandResult result = sqlExecutor.executeCommand(
                SqlCommandRequest.ofWithoutTransaction(connection, sql, sql, db, null));

        if (!result.isSuccess()) {
            return List.of();
        }

        List<ParamRow> list = new ArrayList<>();
        if (result.getRows() != null) {
            for (List<Object> row : result.getRows()) {
                Object specObj = result.getValueByColumnName(row, MysqlRoutineConstants.SPECIFIC_NAME);
                Object nameObj = result.getValueByColumnName(row, MysqlRoutineConstants.PARAMETER_NAME);
                Object dtdObj = result.getValueByColumnName(row, MysqlRoutineConstants.DTD_IDENTIFIER);
                Object posObj = result.getValueByColumnName(row, MysqlRoutineConstants.ORDINAL_POSITION);
                String specName = specObj != null ? specObj.toString() : "";
                String paramName = nameObj != null ? nameObj.toString() : "";
                String dataType = dtdObj != null ? dtdObj.toString().trim() : "";
                int pos = posObj != null ? ((Number) posObj).intValue() : 0;
                list.add(new ParamRow(specName, paramName, dataType, pos));
            }
        }
        return list;
    }

    private record ParamRow(String specName, String paramName, String dataType, int ordinalPosition) {
    }

    private Map<String, List<ParameterInfo>> groupParametersByRoutine(List<ParamRow> rows) {
        Map<String, List<ParamRow>> bySpec = new LinkedHashMap<>();
        for (ParamRow r : rows) {
            bySpec.computeIfAbsent(r.specName(), k -> new ArrayList<>()).add(r);
        }
        Map<String, List<ParameterInfo>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<ParamRow>> e : bySpec.entrySet()) {
            List<ParameterInfo> params = e.getValue().stream()
                    .sorted(Comparator.comparingInt(ParamRow::ordinalPosition))
                    .map(r -> new ParameterInfo(r.paramName(), r.dataType()))
                    .toList();
            result.put(e.getKey(), params);
        }
        return result;
    }

    private String getObjectDdl(Connection connection, String catalog, String objectName,
                               String sqlTemplate, String columnName, String objectType) {
        if (connection == null || StringUtils.isBlank(objectName)) {
            return "";
        }

        String fullName = MysqlIdentifierBuilder.buildFullIdentifier(catalog, objectName);
        String sql = String.format(sqlTemplate, fullName);

        SqlCommandResult result = sqlExecutor.executeCommand(
                SqlCommandRequest.ofWithoutTransaction(connection, sql, sql, catalog, null));

        if (!result.isSuccess()) {
            logger.severe(String.format("Failed to get DDL for %s %s: %s",
                    objectType, fullName, result.getErrorMessage()));
            throw new RuntimeException(String.format("Failed to get %s DDL: %s", objectType, result.getErrorMessage()));
        }

        if (result.getRows() == null || result.getRows().isEmpty()) {
            throw new RuntimeException(String.format("Failed to get %s DDL: No result returned", objectType));
        }

        List<Object> firstRow = result.getRows().get(0);
        Object ddl = result.getValueByColumnName(firstRow, columnName);
        if (ddl == null) {
            throw new RuntimeException(String.format("Failed to get %s DDL: Column '%s' not found in result",
                    objectType, columnName));
        }
        return ddl.toString();
    }

    private void dropObject(Connection connection, String catalog, String objectName,
                           String sqlTemplate, String objectType, boolean useFullIdentifier) {
        if (connection == null || StringUtils.isBlank(objectName)) {
            throw new IllegalArgumentException(String.format("Connection and %s name must not be null or empty", objectType));
        }

        String fullName;
        if (useFullIdentifier) {
            fullName = MysqlIdentifierBuilder.buildFullIdentifier(catalog, objectName);
        } else {
            fullName = MysqlIdentifierEscaper.getInstance().quoteIdentifier(objectName);
        }

        String sql = String.format(sqlTemplate, fullName);

        SqlCommandResult result = sqlExecutor.executeCommand(
                SqlCommandRequest.ofWithoutTransaction(connection, sql, sql, catalog, null));

        if (!result.isSuccess()) {
            logger.severe(String.format("Failed to delete %s %s: %s",
                    objectType, fullName, result.getErrorMessage()));
            throw new RuntimeException(String.format("Failed to delete %s: %s", objectType, result.getErrorMessage()));
        }

        logger.info(String.format("Successfully deleted %s: %s", objectType, fullName));
    }

    @Override
    public void deleteDatabase(Connection connection, String catalog) {
        dropObject(connection, catalog, catalog, MysqlSqlConstants.SQL_DROP_DATABASE,
                DatabaseObjectTypeEnum.DATABASE.getValue(), false);
    }

    @Override
    public void deleteTable(Connection connection, String catalog, String schema, String tableName) {
        dropObject(connection, catalog, tableName, MysqlSqlConstants.SQL_DROP_TABLE,
                DatabaseObjectTypeEnum.TABLE.getValue(), true);
    }

    @Override
    public void deleteView(Connection connection, String catalog, String schema, String viewName) {
        dropObject(connection, catalog, viewName, MysqlSqlConstants.SQL_DROP_VIEW,
                DatabaseObjectTypeEnum.VIEW.getValue(), true);
    }

    @Override
    public void deleteFunction(Connection connection, String catalog, String schema, String functionName) {
        dropObject(connection, catalog, functionName, MysqlSqlConstants.SQL_DROP_FUNCTION,
                DatabaseObjectTypeEnum.FUNCTION.getValue(), true);
    }

    @Override
    public void deleteProcedure(Connection connection, String catalog, String schema, String procedureName) {
        dropObject(connection, catalog, procedureName, MysqlSqlConstants.SQL_DROP_PROCEDURE,
                DatabaseObjectTypeEnum.PROCEDURE.getValue(), true);
    }

    @Override
    public void deleteTrigger(Connection connection, String catalog, String schema, String triggerName) {
        dropObject(connection, catalog, triggerName, MysqlSqlConstants.SQL_DROP_TRIGGER,
                DatabaseObjectTypeEnum.TRIGGER.getValue(), true);
    }
}
