package edu.zsc.ai.plugin.mysql;

import edu.zsc.ai.plugin.base.AbstractDatabasePlugin;
import edu.zsc.ai.plugin.capability.*;
import edu.zsc.ai.plugin.connection.ConnectionConfig;
import edu.zsc.ai.plugin.driver.MavenCoordinates;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.model.db.TableRowValue;
import edu.zsc.ai.plugin.model.metadata.*;
import edu.zsc.ai.plugin.model.sql.SqlType;
import edu.zsc.ai.plugin.model.sql.SqlValidationResult;
import edu.zsc.ai.plugin.mysql.executor.MySQLSqlExecutor;
import edu.zsc.ai.plugin.mysql.manager.*;
import edu.zsc.ai.plugin.mysql.support.MysqlCapabilitySupport;
import edu.zsc.ai.plugin.mysql.validator.MySqlSqlValidator;
import edu.zsc.ai.plugin.sql.DefaultSqlSplitter;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;

public abstract class DefaultMysqlPlugin extends AbstractDatabasePlugin
        implements ConnectionManager, CommandExecutor<SqlCommandRequest, SqlCommandResult>, DatabaseManager,
        TableManager, ViewManager, ColumnManager, IndexManager,
        FunctionManager, ProcedureManager, TriggerManager, SqlSplitter, SqlValidator {

    private final ConnectionManager connectionManager = new MysqlConnectionManager(
            this::getDriverClassName,
            this::getJdbcUrlTemplate,
            this::getDefaultPort
    );
    private final MySQLSqlExecutor sqlExecutor = new MySQLSqlExecutor();
    private final MysqlCapabilitySupport capabilitySupport = new MysqlCapabilitySupport(sqlExecutor);
    private final MySqlSqlValidator sqlValidator = new MySqlSqlValidator();
    private final DatabaseManager databaseManager = new MysqlDatabaseManager(capabilitySupport);
    private final TableManager tableManager = new MysqlTableManager(capabilitySupport);
    private final ViewManager viewManager = new MysqlViewManager(capabilitySupport);
    private final ColumnManager columnManager = new MysqlColumnManager(capabilitySupport);
    private final IndexManager indexManager = new MysqlIndexManager();
    private final FunctionManager functionManager = new MysqlFunctionManager(capabilitySupport);
    private final ProcedureManager procedureManager = new MysqlProcedureManager(capabilitySupport);
    private final TriggerManager triggerManager = new MysqlTriggerManager(capabilitySupport);

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
    public Connection connect(ConnectionConfig config) {
        return connectionManager.connect(config);
    }

    @Override
    public boolean testConnection(ConnectionConfig config) {
        return connectionManager.testConnection(config);
    }

    @Override
    public void closeConnection(Connection connection) {
        connectionManager.closeConnection(connection);
    }

    @Override
    public DatabaseMetaData getMetaData(Connection connection) {
        return connectionManager.getMetaData(connection);
    }

    @Override
    public String getDatabaseProductVersion(Connection connection) {
        return connectionManager.getDatabaseProductVersion(connection);
    }

    @Override
    public String getDbmsInfo(Connection connection) {
        return connectionManager.getDbmsInfo(connection);
    }

    @Override
    public String getDriverInfo(Connection connection) {
        return connectionManager.getDriverInfo(connection);
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
    public SqlValidationResult validate(String sql) {
        return sqlValidator.validate(sql);
    }

    @Override
    public SqlType classifySql(String sql) {
        return sqlValidator.classifySql(sql);
    }


    @Override
    public List<String> getDatabases(Connection connection) {
        return databaseManager.getDatabases(connection);
    }

    @Override
    public void deleteDatabase(Connection connection, String catalog) {
        databaseManager.deleteDatabase(connection, catalog);
    }

    @Override
    public List<String> getTableNames(Connection connection, String catalog, String schema) {
        return tableManager.getTableNames(connection, catalog, schema);
    }

    @Override
    public List<String> searchTables(Connection connection, String catalog, String schema, String tableNamePattern) {
        return tableManager.searchTables(connection, catalog, schema, tableNamePattern);
    }

    @Override
    public long countTables(Connection connection, String catalog, String schema, String tableNamePattern) {
        return tableManager.countTables(connection, catalog, schema, tableNamePattern);
    }

    @Override
    public String getTableDdl(Connection connection, String catalog, String schema, String tableName) {
        return tableManager.getTableDdl(connection, catalog, schema, tableName);
    }

    @Override
    public void deleteTable(Connection connection, String catalog, String schema, String tableName) {
        tableManager.deleteTable(connection, catalog, schema, tableName);
    }

    @Override
    public SqlCommandResult insertRow(Connection connection, String catalog, String schema, String tableName,
                                      List<TableRowValue> values) {
        return tableManager.insertRow(connection, catalog, schema, tableName, values);
    }

    @Override
    public SqlCommandResult deleteRow(Connection connection, String catalog, String schema, String tableName,
                                      List<TableRowValue> matchValues, boolean force) {
        return tableManager.deleteRow(connection, catalog, schema, tableName, matchValues, force);
    }

    @Override
    public SqlCommandResult getTableData(Connection connection, String catalog, String schema,
                                         String tableName, int offset, int pageSize) {
        return tableManager.getTableData(connection, catalog, schema, tableName, offset, pageSize);
    }

    @Override
    public long getTableDataCount(Connection connection, String catalog, String schema, String tableName) {
        return tableManager.getTableDataCount(connection, catalog, schema, tableName);
    }

    @Override
    public SqlCommandResult getTableData(Connection connection, String catalog, String schema, String tableName,
                                         int offset, int pageSize, String whereClause,
                                         String orderByColumn, String orderByDirection) {
        return tableManager.getTableData(
                connection,
                catalog,
                schema,
                tableName,
                offset,
                pageSize,
                whereClause,
                orderByColumn,
                orderByDirection
        );
    }

    @Override
    public long getTableDataCount(Connection connection, String catalog, String schema,
                                  String tableName, String whereClause) {
        return tableManager.getTableDataCount(connection, catalog, schema, tableName, whereClause);
    }

    @Override
    public List<String> getViews(Connection connection, String catalog, String schema) {
        return viewManager.getViews(connection, catalog, schema);
    }

    @Override
    public List<String> searchViews(Connection connection, String catalog, String schema, String viewNamePattern) {
        return viewManager.searchViews(connection, catalog, schema, viewNamePattern);
    }

    @Override
    public long countViews(Connection connection, String catalog, String schema, String viewNamePattern) {
        return viewManager.countViews(connection, catalog, schema, viewNamePattern);
    }

    @Override
    public String getViewDdl(Connection connection, String catalog, String schema, String viewName) {
        return viewManager.getViewDdl(connection, catalog, schema, viewName);
    }

    @Override
    public void deleteView(Connection connection, String catalog, String schema, String viewName) {
        viewManager.deleteView(connection, catalog, schema, viewName);
    }

    @Override
    public SqlCommandResult getViewData(Connection connection, String catalog, String schema,
                                        String viewName, int offset, int pageSize) {
        return viewManager.getViewData(connection, catalog, schema, viewName, offset, pageSize);
    }

    @Override
    public long getViewDataCount(Connection connection, String catalog, String schema, String viewName) {
        return viewManager.getViewDataCount(connection, catalog, schema, viewName);
    }

    @Override
    public SqlCommandResult getViewData(Connection connection, String catalog, String schema, String viewName,
                                        int offset, int pageSize, String whereClause,
                                        String orderByColumn, String orderByDirection) {
        return viewManager.getViewData(
                connection,
                catalog,
                schema,
                viewName,
                offset,
                pageSize,
                whereClause,
                orderByColumn,
                orderByDirection
        );
    }

    @Override
    public long getViewDataCount(Connection connection, String catalog, String schema,
                                 String viewName, String whereClause) {
        return viewManager.getViewDataCount(connection, catalog, schema, viewName, whereClause);
    }

    @Override
    public List<ColumnMetadata> getColumns(Connection connection, String catalog, String schema, String tableOrViewName) {
        return columnManager.getColumns(connection, catalog, schema, tableOrViewName);
    }

    @Override
    public List<IndexMetadata> getIndexes(Connection connection, String catalog, String schema, String tableName) {
        return indexManager.getIndexes(connection, catalog, schema, tableName);
    }

    @Override
    public List<FunctionMetadata> getFunctions(Connection connection, String catalog, String schema) {
        return functionManager.getFunctions(connection, catalog, schema);
    }

    @Override
    public List<FunctionMetadata> searchFunctions(Connection connection, String catalog, String schema, String functionNamePattern) {
        return functionManager.searchFunctions(connection, catalog, schema, functionNamePattern);
    }

    @Override
    public long countFunctions(Connection connection, String catalog, String schema, String functionNamePattern) {
        return functionManager.countFunctions(connection, catalog, schema, functionNamePattern);
    }

    @Override
    public String getFunctionDdl(Connection connection, String catalog, String schema, String functionName) {
        return functionManager.getFunctionDdl(connection, catalog, schema, functionName);
    }

    @Override
    public void deleteFunction(Connection connection, String catalog, String schema, String functionName) {
        functionManager.deleteFunction(connection, catalog, schema, functionName);
    }

    @Override
    public List<ProcedureMetadata> getProcedures(Connection connection, String catalog, String schema) {
        return procedureManager.getProcedures(connection, catalog, schema);
    }

    @Override
    public List<ProcedureMetadata> searchProcedures(Connection connection, String catalog, String schema,
                                                    String procedureNamePattern) {
        return procedureManager.searchProcedures(connection, catalog, schema, procedureNamePattern);
    }

    @Override
    public long countProcedures(Connection connection, String catalog, String schema, String procedureNamePattern) {
        return procedureManager.countProcedures(connection, catalog, schema, procedureNamePattern);
    }

    @Override
    public String getProcedureDdl(Connection connection, String catalog, String schema, String procedureName) {
        return procedureManager.getProcedureDdl(connection, catalog, schema, procedureName);
    }

    @Override
    public void deleteProcedure(Connection connection, String catalog, String schema, String procedureName) {
        procedureManager.deleteProcedure(connection, catalog, schema, procedureName);
    }

    @Override
    public List<TriggerMetadata> getTriggers(Connection connection, String catalog, String schema, String tableName) {
        return triggerManager.getTriggers(connection, catalog, schema, tableName);
    }

    @Override
    public List<TriggerMetadata> searchTriggers(Connection connection, String catalog, String schema,
                                                String tableName, String triggerNamePattern) {
        return triggerManager.searchTriggers(connection, catalog, schema, tableName, triggerNamePattern);
    }

    @Override
    public String getTriggerDdl(Connection connection, String catalog, String schema, String triggerName) {
        return triggerManager.getTriggerDdl(connection, catalog, schema, triggerName);
    }

    @Override
    public void deleteTrigger(Connection connection, String catalog, String schema, String triggerName) {
        triggerManager.deleteTrigger(connection, catalog, schema, triggerName);
    }
}
