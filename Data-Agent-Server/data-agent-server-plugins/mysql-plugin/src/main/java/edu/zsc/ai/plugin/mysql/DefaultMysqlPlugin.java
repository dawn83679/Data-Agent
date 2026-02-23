package edu.zsc.ai.plugin.mysql;

import edu.zsc.ai.plugin.base.AbstractDatabasePlugin;
import edu.zsc.ai.plugin.capability.*;
import edu.zsc.ai.plugin.connection.ConnectionConfig;
import edu.zsc.ai.plugin.connection.JdbcConnectionBuilder;
import edu.zsc.ai.plugin.driver.DriverLoader;
import edu.zsc.ai.plugin.driver.MavenCoordinates;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.mysql.constant.MysqlColumnConstants;
import edu.zsc.ai.plugin.constant.IsNullableEnum;
import edu.zsc.ai.plugin.mysql.value.MySQLDataTypeEnum;
import edu.zsc.ai.plugin.mysql.constant.MysqlRoutineConstants;
import edu.zsc.ai.plugin.mysql.constant.MysqlShowColumnConstants;
import edu.zsc.ai.plugin.mysql.constant.MysqlSqlConstants;
import edu.zsc.ai.plugin.mysql.constant.MysqlTriggerConstants;
import edu.zsc.ai.plugin.mysql.connection.MysqlJdbcConnectionBuilder;
import edu.zsc.ai.plugin.mysql.executor.MySQLSqlExecutor;

import edu.zsc.ai.plugin.model.metadata.ColumnMetadata;
import edu.zsc.ai.plugin.model.metadata.FunctionMetadata;
import edu.zsc.ai.plugin.model.metadata.ParameterInfo;
import edu.zsc.ai.plugin.model.metadata.ProcedureMetadata;
import edu.zsc.ai.plugin.model.metadata.TriggerMetadata;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public abstract class DefaultMysqlPlugin extends AbstractDatabasePlugin
        implements ConnectionProvider, CommandExecutor<SqlCommandRequest, SqlCommandResult>, DatabaseProvider,
        SchemaProvider, TableProvider, ViewProvider, ColumnProvider, IndexProvider,
        FunctionProvider, ProcedureProvider, TriggerProvider {

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
        if (connection == null || tableOrViewName == null || tableOrViewName.isEmpty()) {
            return List.of();
        }
        String db = catalog != null && !catalog.isEmpty() ? catalog : schema;
        if (db == null || db.isEmpty()) {
            return List.of();
        }
        String escapedDb = db.replace("'", "''");
        String escapedTable = tableOrViewName.replace("'", "''");
        String sql = String.format(MysqlSqlConstants.SQL_LIST_COLUMNS, escapedDb, escapedTable);

        SqlCommandRequest request = new SqlCommandRequest();
        request.setConnection(connection);
        request.setOriginalSql(sql);
        request.setExecuteSql(sql);
        request.setDatabase(db);
        request.setNeedTransaction(false);

        SqlCommandResult result = sqlExecutor.executeCommand(request);
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
        if (connection == null || tableName == null || tableName.isEmpty()) {
            return "";
        }

        String fullTableName = (catalog != null && !catalog.isEmpty())
                ? String.format("`%s`.`%s`", catalog, tableName)
                : String.format("`%s`", tableName);
        String sql = String.format(MysqlSqlConstants.SQL_SHOW_CREATE_TABLE, fullTableName);

        SqlCommandRequest request = new SqlCommandRequest();
        request.setConnection(connection);
        request.setOriginalSql(sql);
        request.setExecuteSql(sql);
        request.setDatabase(catalog);
        request.setNeedTransaction(false);

        SqlCommandResult result = sqlExecutor.executeCommand(request);

        if (!result.isSuccess()) {
            logger.severe(String.format("Failed to get DDL for table %s: %s",
                    fullTableName, result.getErrorMessage()));
            throw new RuntimeException("Failed to get table DDL: " + result.getErrorMessage());
        }

        if (result.getRows() == null || result.getRows().isEmpty()) {
            throw new RuntimeException("Failed to get table DDL: No result returned");
        }

        List<Object> firstRow = result.getRows().get(0);
        Object ddl = result.getValueByColumnName(firstRow, MysqlShowColumnConstants.CREATE_TABLE);
        if (ddl == null) {
            throw new RuntimeException("Failed to get table DDL: Column '" + MysqlShowColumnConstants.CREATE_TABLE + "' not found in result");
        }
        return ddl.toString();
    }

    @Override
    public String getViewDdl(Connection connection, String catalog, String schema, String viewName) {
        if (connection == null || viewName == null || viewName.isEmpty()) {
            return "";
        }

        String fullViewName = (catalog != null && !catalog.isEmpty())
                ? String.format("`%s`.`%s`", catalog, viewName)
                : String.format("`%s`", viewName);
        String sql = String.format(MysqlSqlConstants.SQL_SHOW_CREATE_VIEW, fullViewName);

        SqlCommandRequest request = new SqlCommandRequest();
        request.setConnection(connection);
        request.setOriginalSql(sql);
        request.setExecuteSql(sql);
        request.setDatabase(catalog);
        request.setNeedTransaction(false);

        SqlCommandResult result = sqlExecutor.executeCommand(request);

        if (!result.isSuccess()) {
            logger.severe(String.format("Failed to get DDL for view %s: %s",
                    fullViewName, result.getErrorMessage()));
            throw new RuntimeException("Failed to get view DDL: " + result.getErrorMessage());
        }

        if (result.getRows() == null || result.getRows().isEmpty()) {
            throw new RuntimeException("Failed to get view DDL: No result returned");
        }

        List<Object> firstRow = result.getRows().get(0);
        Object ddl = result.getValueByColumnName(firstRow, MysqlShowColumnConstants.CREATE_VIEW);
        if (ddl == null) {
            throw new RuntimeException("Failed to get view DDL: Column '" + MysqlShowColumnConstants.CREATE_VIEW + "' not found in result");
        }
        return ddl.toString();
    }

    @Override
    public String getFunctionDdl(Connection connection, String catalog, String schema, String functionName) {
        if (connection == null || functionName == null || functionName.isEmpty()) {
            return "";
        }

        String fullName = (catalog != null && !catalog.isEmpty())
                ? String.format("`%s`.`%s`", catalog, functionName)
                : String.format("`%s`", functionName);
        String sql = String.format(MysqlSqlConstants.SQL_SHOW_CREATE_FUNCTION, fullName);

        SqlCommandRequest request = new SqlCommandRequest();
        request.setConnection(connection);
        request.setOriginalSql(sql);
        request.setExecuteSql(sql);
        request.setDatabase(catalog);
        request.setNeedTransaction(false);

        SqlCommandResult result = sqlExecutor.executeCommand(request);

        if (!result.isSuccess()) {
            logger.severe(String.format("Failed to get DDL for function %s: %s",
                    fullName, result.getErrorMessage()));
            throw new RuntimeException("Failed to get function DDL: " + result.getErrorMessage());
        }

        if (result.getRows() == null || result.getRows().isEmpty()) {
            throw new RuntimeException("Failed to get function DDL: No result returned");
        }

        List<Object> firstRow = result.getRows().get(0);
        Object ddl = result.getValueByColumnName(firstRow, MysqlShowColumnConstants.CREATE_FUNCTION);
        if (ddl == null) {
            throw new RuntimeException("Failed to get function DDL: Column '" + MysqlShowColumnConstants.CREATE_FUNCTION + "' not found in result");
        }
        return ddl.toString();
    }

    @Override
    public String getProcedureDdl(Connection connection, String catalog, String schema, String procedureName) {
        if (connection == null || procedureName == null || procedureName.isEmpty()) {
            return "";
        }

        String fullName = (catalog != null && !catalog.isEmpty())
                ? String.format("`%s`.`%s`", catalog, procedureName)
                : String.format("`%s`", procedureName);
        String sql = String.format(MysqlSqlConstants.SQL_SHOW_CREATE_PROCEDURE, fullName);

        SqlCommandRequest request = new SqlCommandRequest();
        request.setConnection(connection);
        request.setOriginalSql(sql);
        request.setExecuteSql(sql);
        request.setDatabase(catalog);
        request.setNeedTransaction(false);

        SqlCommandResult result = sqlExecutor.executeCommand(request);

        if (!result.isSuccess()) {
            logger.severe(String.format("Failed to get DDL for procedure %s: %s",
                    fullName, result.getErrorMessage()));
            throw new RuntimeException("Failed to get procedure DDL: " + result.getErrorMessage());
        }

        if (result.getRows() == null || result.getRows().isEmpty()) {
            throw new RuntimeException("Failed to get procedure DDL: No result returned");
        }

        List<Object> firstRow = result.getRows().get(0);
        Object ddl = result.getValueByColumnName(firstRow, MysqlShowColumnConstants.CREATE_PROCEDURE);
        if (ddl == null) {
            throw new RuntimeException("Failed to get procedure DDL: Column '" + MysqlShowColumnConstants.CREATE_PROCEDURE + "' not found in result");
        }
        return ddl.toString();
    }

    @Override
    public String getTriggerDdl(Connection connection, String catalog, String schema, String triggerName) {
        if (connection == null || triggerName == null || triggerName.isEmpty()) {
            return "";
        }

        String fullName = (catalog != null && !catalog.isEmpty())
                ? String.format("`%s`.`%s`", catalog, triggerName)
                : String.format("`%s`", triggerName);
        String sql = String.format(MysqlSqlConstants.SQL_SHOW_CREATE_TRIGGER, fullName);

        SqlCommandRequest request = new SqlCommandRequest();
        request.setConnection(connection);
        request.setOriginalSql(sql);
        request.setExecuteSql(sql);
        request.setDatabase(catalog);
        request.setNeedTransaction(false);

        SqlCommandResult result = sqlExecutor.executeCommand(request);

        if (!result.isSuccess()) {
            logger.severe(String.format("Failed to get DDL for trigger %s: %s",
                    fullName, result.getErrorMessage()));
            throw new RuntimeException("Failed to get trigger DDL: " + result.getErrorMessage());
        }

        if (result.getRows() == null || result.getRows().isEmpty()) {
            throw new RuntimeException("Failed to get trigger DDL: No result returned");
        }

        List<Object> firstRow = result.getRows().get(0);
        Object ddl = result.getValueByColumnName(firstRow, MysqlShowColumnConstants.SQL_ORIGINAL_STATEMENT);
        if (ddl == null) {
            throw new RuntimeException("Failed to get trigger DDL: Column '" + MysqlShowColumnConstants.SQL_ORIGINAL_STATEMENT + "' not found in result");
        }
        return ddl.toString();
    }

    @Override
    public ResultSet getTableData(Connection connection, String catalog, String schema, String tableName, int offset, int pageSize) {
        if (connection == null || tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("Connection and table name must not be null or empty");
        }

        String fullTableName = (catalog != null && !catalog.isEmpty())
                ? String.format("`%s`.`%s`", catalog, tableName)
                : String.format("`%s`", tableName);

        String sql = String.format(MysqlSqlConstants.SQL_SELECT_TABLE_DATA, fullTableName, pageSize, offset);

        try {
            return connection.prepareStatement(sql).executeQuery();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get table data: " + e.getMessage(), e);
        }
    }

    @Override
    public long getTableDataCount(Connection connection, String catalog, String schema, String tableName) {
        if (connection == null || tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("Connection and table name must not be null or empty");
        }

        String fullTableName = (catalog != null && !catalog.isEmpty())
                ? String.format("`%s`.`%s`", catalog, tableName)
                : String.format("`%s`", tableName);

        String sql = String.format(MysqlSqlConstants.SQL_COUNT_TABLE_DATA, fullTableName);

        try {
            ResultSet rs = connection.prepareStatement(sql).executeQuery();
            if (rs.next()) {
                long count = rs.getLong("total");
                rs.close();
                return count;
            }
            rs.close();
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get table data count: " + e.getMessage(), e);
        }
    }

    @Override
    public ResultSet getViewData(Connection connection, String catalog, String schema, String viewName, int offset, int pageSize) {
        return getTableData(connection, catalog, schema, viewName, offset, pageSize);
    }

    @Override
    public long getViewDataCount(Connection connection, String catalog, String schema, String viewName) {
        return getTableDataCount(connection, catalog, schema, viewName);
    }

    @Override
    public List<TriggerMetadata> getTriggers(Connection connection, String catalog, String schema, String tableName) {
        if (connection == null) {
            return List.of();
        }
        String db = catalog != null && !catalog.isEmpty() ? catalog : schema;
        if (db == null || db.isEmpty()) {
            return List.of();
        }
        String escapedDb = db.replace("'", "''");
        String sql = String.format(MysqlSqlConstants.SQL_LIST_TRIGGERS, escapedDb);
        if (tableName != null && !tableName.isEmpty()) {
            String escapedTable = tableName.replace("'", "''");
            sql += MysqlSqlConstants.SQL_TRIGGER_FILTER_BY_TABLE + escapedTable + "'";
        }

        SqlCommandRequest request = new SqlCommandRequest();
        request.setConnection(connection);
        request.setOriginalSql(sql);
        request.setExecuteSql(sql);
        request.setDatabase(db);
        request.setNeedTransaction(false);

        SqlCommandResult result = sqlExecutor.executeCommand(request);
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
                if (!name.isEmpty()) {
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
        String db = catalog != null && !catalog.isEmpty() ? catalog : schema;
        if (db == null || db.isEmpty()) {
            return List.of();
        }
        String escapedDb = db.replace("'", "''");
        String sql = String.format(MysqlSqlConstants.SQL_LIST_FUNCTIONS, escapedDb);

        SqlCommandRequest request = new SqlCommandRequest();
        request.setConnection(connection);
        request.setOriginalSql(sql);
        request.setExecuteSql(sql);
        request.setDatabase(db);
        request.setNeedTransaction(false);

        SqlCommandResult result = sqlExecutor.executeCommand(request);
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
        String db = catalog != null && !catalog.isEmpty() ? catalog : schema;
        if (db == null || db.isEmpty()) {
            return List.of();
        }
        String escapedDb = db.replace("'", "''");
        String sql = String.format(MysqlSqlConstants.SQL_LIST_PROCEDURES, escapedDb);

        SqlCommandRequest request = new SqlCommandRequest();
        request.setConnection(connection);
        request.setOriginalSql(sql);
        request.setExecuteSql(sql);
        request.setDatabase(db);
        request.setNeedTransaction(false);

        SqlCommandResult result = sqlExecutor.executeCommand(request);
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
                if (!name.isEmpty() && !proceduresByName.containsKey(specName)) {
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
            if (!inClause.isEmpty()) inClause.append(',');
            inClause.append("'").append(sn.replace("'", "''")).append("'");
        }
        String escapedDb = db.replace("'", "''");
        String sql = String.format(MysqlSqlConstants.SQL_FETCH_PARAMETERS, escapedDb, inClause);

        SqlCommandRequest request = new SqlCommandRequest();
        request.setConnection(connection);
        request.setOriginalSql(sql);
        request.setExecuteSql(sql);
        request.setDatabase(db);
        request.setNeedTransaction(false);

        SqlCommandResult result = sqlExecutor.executeCommand(request);
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

    @Override
    public String exportDatabaseDdl(Connection connection, String databaseName) {
        if (connection == null || databaseName == null || databaseName.isEmpty()) {
            throw new IllegalArgumentException("Connection and database name must not be null or empty");
        }

        StringBuilder sb = new StringBuilder();

        // Add header like Navicat
        sb.append("--\n");
        sb.append("-- Database: ").append(databaseName).append("\n");
        sb.append("-- Please select the database first before executing this script\n");
        sb.append("--\n\n");

        // Add SET statements like Navicat
        sb.append("SET NAMES utf8mb4;\n");
        sb.append("SET FOREIGN_KEY_CHECKS = 0;\n\n");

        // Export all tables DDL (direct JDBC query)
        String tablesSql = "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = '" + databaseName + "' AND TABLE_TYPE = 'BASE TABLE'";
        try (ResultSet rs = connection.prepareStatement(tablesSql).executeQuery()) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                String tableSql = "SHOW CREATE TABLE `" + tableName + "`";
                try (ResultSet rs2 = connection.prepareStatement(tableSql).executeQuery()) {
                    if (rs2.next()) {
                        sb.append("DROP TABLE IF EXISTS `").append(tableName).append("`;\n");
                        sb.append(rs2.getString(2)).append(";\n\n");
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to export tables: " + e.getMessage());
        }

        // Export all table data
        String tableDataSql = "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = '" + databaseName + "' AND TABLE_TYPE = 'BASE TABLE'";
        try (ResultSet rs = connection.prepareStatement(tableDataSql).executeQuery()) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                sb.append("-- Data for table ").append(tableName).append("\n");
                exportTableData(connection, tableName, sb);
                sb.append("\n");
            }
        } catch (Exception e) {
            logger.warning("Failed to export table data: " + e.getMessage());
        }

        // Export all views (direct JDBC query)
        String viewsSql = "SELECT TABLE_NAME FROM information_schema.VIEWS WHERE TABLE_SCHEMA = '" + databaseName + "'";
        try (ResultSet rs = connection.prepareStatement(viewsSql).executeQuery()) {
            while (rs.next()) {
                String viewName = rs.getString("TABLE_NAME");
                String viewSql = "SHOW CREATE VIEW `" + viewName + "`";
                try (ResultSet rs2 = connection.prepareStatement(viewSql).executeQuery()) {
                    if (rs2.next()) {
                        sb.append("-- ----------------------------\n");
                        sb.append("-- View structure for ").append(viewName).append("\n");
                        sb.append("-- ----------------------------\n");
                        sb.append("DROP VIEW IF EXISTS `").append(viewName).append("`;\n");
                        sb.append(rs2.getString(2)).append(";\n\n");
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to export views: " + e.getMessage());
        }

        // Export all triggers (direct JDBC query)
        String triggerSql = "SELECT TRIGGER_NAME, EVENT_OBJECT_TABLE FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = '" + databaseName + "'";
        try (ResultSet rs = connection.prepareStatement(triggerSql).executeQuery()) {
            while (rs.next()) {
                String triggerName = rs.getString("TRIGGER_NAME");
                String tableName = rs.getString("EVENT_OBJECT_TABLE");
                String showSql = "SHOW CREATE TRIGGER `" + triggerName + "`";
                try (ResultSet rs2 = connection.prepareStatement(showSql).executeQuery()) {
                    if (rs2.next()) {
                        sb.append("-- ----------------------------\n");
                        sb.append("-- Triggers structure for table ").append(tableName).append("\n");
                        sb.append("-- ----------------------------\n");
                        sb.append("DROP TRIGGER IF EXISTS `").append(triggerName).append("`;\n");
                        sb.append(rs2.getString("SQL Original Statement"));
                        sb.append(";\n\n");
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to export triggers: " + e.getMessage());
        }

        // Export all functions (direct JDBC query)
        String funcSql = "SELECT ROUTINE_NAME FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA = '" + databaseName + "' AND ROUTINE_TYPE = 'FUNCTION'";
        try (ResultSet rs = connection.prepareStatement(funcSql).executeQuery()) {
            while (rs.next()) {
                String funcName = rs.getString("ROUTINE_NAME");
                String showSql = "SHOW CREATE FUNCTION `" + funcName + "`";
                try (ResultSet rs2 = connection.prepareStatement(showSql).executeQuery()) {
                    if (rs2.next()) {
                        sb.append("-- ----------------------------\n");
                        sb.append("-- Function structure for ").append(funcName).append("\n");
                        sb.append("-- ----------------------------\n");
                        sb.append("DROP FUNCTION IF EXISTS `").append(funcName).append("`;\n");
                        sb.append(rs2.getString("Create Function"));
                        sb.append(";\n\n");
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to export functions: " + e.getMessage());
        }

        // Export all procedures (direct JDBC query)
        String procSql = "SELECT ROUTINE_NAME FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA = '" + databaseName + "' AND ROUTINE_TYPE = 'PROCEDURE'";
        try (ResultSet rs = connection.prepareStatement(procSql).executeQuery()) {
            while (rs.next()) {
                String procName = rs.getString("ROUTINE_NAME");
                String showSql = "SHOW CREATE PROCEDURE `" + procName + "`";
                try (ResultSet rs2 = connection.prepareStatement(showSql).executeQuery()) {
                    if (rs2.next()) {
                        sb.append("-- ----------------------------\n");
                        sb.append("-- Procedure structure for ").append(procName).append("\n");
                        sb.append("-- ----------------------------\n");
                        sb.append("DROP PROCEDURE IF EXISTS `").append(procName).append("`;\n");
                        sb.append(rs2.getString("Create Procedure"));
                        sb.append(";\n\n");
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to export procedures: " + e.getMessage());
        }

        // Add final SET statement like Navicat
        sb.append("\nSET FOREIGN_KEY_CHECKS = 1;");

        return sb.toString();
    }

    private void exportTableData(Connection connection, String tableName, StringBuilder sb) {
        String sql = "SELECT * FROM `" + tableName + "`";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                StringBuilder values = new StringBuilder();
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) values.append(", ");

                    Object value = rs.getObject(i);
                    if (value == null) {
                        values.append("NULL");
                    } else if (value instanceof Boolean) {
                        // Convert Boolean to 0/1 for MySQL tinyint(1)
                        values.append(((Boolean) value) ? 1 : 0);
                    } else if (value instanceof Number) {
                        values.append(String.valueOf(value));
                    } else if (value instanceof java.util.Date) {
                        values.append("'").append(value).append("'");
                    } else {
                        // Escape single quotes and backslashes
                        String str = value.toString().replace("\\", "\\\\").replace("'", "\\'");
                        values.append("'").append(str).append("'");
                    }
                }

                sb.append("INSERT INTO `").append(tableName).append("` VALUES (").append(values).append(");\n");
            }
        } catch (SQLException e) {
            logger.warning("Failed to export data for table " + tableName + ": " + e.getMessage());
        }
    }

    @Override
    public List<String> exportAllTableDdls(Connection connection, String databaseName) {
        if (connection == null || databaseName == null || databaseName.isEmpty()) {
            throw new IllegalArgumentException("Connection and database name must not be null or empty");
        }

        List<String> ddlList = new ArrayList<>();
        List<String> tables = getTableNames(connection, databaseName, null);

        for (String tableName : tables) {
            String sql = String.format(MysqlSqlConstants.SQL_SHOW_CREATE_TABLE, tableName);
            try (ResultSet rs = connection.prepareStatement(sql).executeQuery()) {
                if (rs.next()) {
                    ddlList.add(rs.getString(2));
                }
            } catch (SQLException e) {
                logger.warning("Failed to export DDL for table " + tableName + ": " + e.getMessage());
            }
        }

        return ddlList;
    }

    @Override
    public void executeSqlScript(Connection connection, String sqlScript) {
        if (connection == null || sqlScript == null || sqlScript.isEmpty()) {
            throw new IllegalArgumentException("Connection and SQL script must not be null or empty");
        }

        try (Statement stmt = connection.createStatement()) {
            // Split by semicolon, but only when not inside quotes
            List<String> statements = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inSingleQuote = false;
            boolean inDoubleQuote = false;
            boolean inBacktick = false;

            for (int i = 0; i < sqlScript.length(); i++) {
                char c = sqlScript.charAt(i);

                // Handle escape character
                if (c == '\\' && i + 1 < sqlScript.length()) {
                    current.append(c);
                    current.append(sqlScript.charAt(++i));
                    continue;
                }

                // Handle quote toggles
                if (c == '\'' && !inDoubleQuote && !inBacktick) {
                    inSingleQuote = !inSingleQuote;
                } else if (c == '"' && !inSingleQuote && !inBacktick) {
                    inDoubleQuote = !inDoubleQuote;
                } else if (c == '`' && !inSingleQuote && !inDoubleQuote) {
                    inBacktick = !inBacktick;
                }

                // Split on semicolon when not inside any quote
                if (c == ';' && !inSingleQuote && !inDoubleQuote && !inBacktick) {
                    String trimmed = current.toString().trim();
                    // Remove comments
                    trimmed = removeComments(trimmed);
                    if (!trimmed.isEmpty()) {
                        statements.add(trimmed);
                    }
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }

            // Add remaining statement
            String trimmed = current.toString().trim();
            trimmed = removeComments(trimmed);
            if (!trimmed.isEmpty()) {
                statements.add(trimmed);
            }

            // Execute each statement
            for (String sql : statements) {
                if (!sql.isEmpty()) {
                    // Add IF NOT EXISTS to avoid errors for existing databases/tables
                    String modifiedSql = sql;
                    if (sql.toUpperCase().startsWith("CREATE DATABASE")) {
                        modifiedSql = sql.replaceFirst("CREATE DATABASE", "CREATE DATABASE IF NOT EXISTS");
                    } else if (sql.toUpperCase().startsWith("CREATE TABLE")) {
                        modifiedSql = sql.replaceFirst("CREATE TABLE", "CREATE TABLE IF NOT EXISTS");
                    }
                    try {
                        stmt.execute(modifiedSql);
                    } catch (SQLException e) {
                        // Log but continue with next statement
                        logger.warning("Failed to execute SQL: " + modifiedSql + " - " + e.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQL script: " + e.getMessage(), e);
        }
    }

    private String removeComments(String sql) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }
        // Remove single-line comments (-- ...)
        sql = sql.replaceAll("--[^\\n]*", "");
        // Remove multi-line comments (/* ... */)
        sql = sql.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        return sql.trim();
    }

    @Override
    public List<String> getCharacterSets(Connection connection) {
        if (connection == null) {
            return List.of();
        }
        String sql = MysqlSqlConstants.SQL_SHOW_CHARACTER_SET;
        SqlCommandRequest request = new SqlCommandRequest();
        request.setConnection(connection);
        request.setOriginalSql(sql);
        request.setExecuteSql(sql);
        request.setNeedTransaction(false);

        SqlCommandResult result = sqlExecutor.executeCommand(request);
        if (!result.isSuccess()) {
            logger.severe("Failed to get character sets: " + result.getErrorMessage());
            throw new RuntimeException("Failed to get character sets: " + result.getErrorMessage());
        }

        List<String> charsets = new ArrayList<>();
        if (result.getRows() != null) {
            for (List<Object> row : result.getRows()) {
                Object charsetObj = result.getValueByColumnName(row, "Charset");
                if (charsetObj != null) {
                    charsets.add(charsetObj.toString());
                }
            }
        }
        return charsets;
    }

    @Override
    public List<String> getCollations(Connection connection, String charset) {
        if (connection == null || charset == null || charset.isEmpty()) {
            return List.of();
        }
        String sql = String.format(MysqlSqlConstants.SQL_SHOW_COLLATION, charset);
        SqlCommandRequest request = new SqlCommandRequest();
        request.setConnection(connection);
        request.setOriginalSql(sql);
        request.setExecuteSql(sql);
        request.setNeedTransaction(false);

        SqlCommandResult result = sqlExecutor.executeCommand(request);
        if (!result.isSuccess()) {
            logger.severe("Failed to get collations for charset " + charset + ": " + result.getErrorMessage());
            throw new RuntimeException("Failed to get collations: " + result.getErrorMessage());
        }

        List<String> collations = new ArrayList<>();
        if (result.getRows() != null) {
            for (List<Object> row : result.getRows()) {
                Object collationObj = result.getValueByColumnName(row, "Collation");
                if (collationObj != null) {
                    collations.add(collationObj.toString());
                }
            }
        }
        return collations;
    }

    @Override
    public void createDatabase(Connection connection, String databaseName, String charset, String collation) {
        if (connection == null || databaseName == null || databaseName.isEmpty()) {
            throw new IllegalArgumentException("Connection and database name must not be null or empty");
        }

        StringBuilder sql = new StringBuilder("CREATE DATABASE `");
        sql.append(databaseName.replace("`", "``"));
        sql.append("`");

        if (charset != null && !charset.isEmpty()) {
            sql.append(" CHARACTER SET ");
            sql.append(charset);
            if (collation != null && !collation.isEmpty()) {
                sql.append(" COLLATE ");
                sql.append(collation);
            }
        }

        SqlCommandRequest request = new SqlCommandRequest();
        request.setConnection(connection);
        request.setOriginalSql(sql.toString());
        request.setExecuteSql(sql.toString());
        request.setNeedTransaction(false);

        SqlCommandResult result = sqlExecutor.executeCommand(request);

        if (!result.isSuccess()) {
            logger.severe("Failed to create database: " + result.getErrorMessage());
            throw new RuntimeException("Failed to create database: " + result.getErrorMessage());
        }

        logger.info("Database created successfully: databaseName=" + databaseName + ", charset=" + charset + ", collation=" + collation);
    }

    @Override
    public boolean databaseExists(Connection connection, String databaseName) {
        if (connection == null || databaseName == null || databaseName.isEmpty()) {
            return false;
        }

        String sql = String.format(MysqlSqlConstants.SQL_SHOW_DATABASES_LIKE, databaseName.replace("'", "''"));
        SqlCommandRequest request = new SqlCommandRequest();
        request.setConnection(connection);
        request.setOriginalSql(sql);
        request.setExecuteSql(sql);
        request.setNeedTransaction(false);

        SqlCommandResult result = sqlExecutor.executeCommand(request);
        if (!result.isSuccess()) {
            // If query fails, assume database doesn't exist
            return false;
        }

        return result.getRows() != null && !result.getRows().isEmpty();
    }
}
