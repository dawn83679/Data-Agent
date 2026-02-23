package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.constant.JdbcMetaDataConstants;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface DatabaseProvider {

    default List<String> getDatabases(Connection connection) {
        try {
            List<String> list = new ArrayList<>();
            try (ResultSet rs = connection.getMetaData().getCatalogs()) {
                while (rs.next()) {
                    String name = rs.getString(JdbcMetaDataConstants.TABLE_CAT);
                    if (StringUtils.isNotBlank(name)) {
                        list.add(name);
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list databases: " + e.getMessage(), e);
        }
    }

    default void deleteDatabase(Connection connection, String pluginId, String databaseName) {
        Logger log = LoggerFactory.getLogger(DatabaseProvider.class);
        CommandExecutor<SqlCommandRequest, SqlCommandResult> executor = DefaultPluginManager.getInstance()
                .getSqlCommandExecutorByPluginId(pluginId);

        String dropSql = String.format("DROP DATABASE `%s`", databaseName);

        SqlCommandRequest pluginRequest = new SqlCommandRequest();
        pluginRequest.setConnection(connection);
        pluginRequest.setOriginalSql(dropSql);
        pluginRequest.setExecuteSql(dropSql);
        pluginRequest.setDatabase(databaseName);
        pluginRequest.setNeedTransaction(false);

        SqlCommandResult result = executor.executeCommand(pluginRequest);

        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to delete database: " + result.getErrorMessage());
        }

        log.info("Database deleted successfully: databaseName={}", databaseName);
    }

    /**
     * Export database DDL (CREATE DATABASE statement)
     * @param connection database connection
     * @param databaseName database name
     * @return CREATE DATABASE statement
     */
    default String exportDatabaseDdl(Connection connection, String databaseName) {
        throw new UnsupportedOperationException("Plugin does not support exporting database DDL");
    }

    /**
     * Export all table DDLs in a database
     * @param connection database connection
     * @param databaseName database name
     * @return list of CREATE TABLE statements
     */
    default List<String> exportAllTableDdls(Connection connection, String databaseName) {
        throw new UnsupportedOperationException("Plugin does not support exporting table DDLs");
    }

    /**
     * Execute SQL script for import
     * @param connection database connection
     * @param sqlScript SQL script to execute
     * @return success or not
     */
    default void executeSqlScript(Connection connection, String sqlScript) {
        throw new UnsupportedOperationException("Plugin does not support executing SQL script");
    }

    /**
     * Get list of available character sets for creating database
     * @param connection database connection
     * @return list of character set names
     */
    default List<String> getCharacterSets(Connection connection) {
        throw new UnsupportedOperationException("Plugin does not support getting character sets");
    }

    /**
     * Get list of available collations for a given character set
     * @param connection database connection
     * @param charset character set name
     * @return list of collation names
     */
    default List<String> getCollations(Connection connection, String charset) {
        throw new UnsupportedOperationException("Plugin does not support getting collations");
    }

    /**
     * Create a new database
     * @param connection database connection
     * @param databaseName database name
     * @param charset character set
     * @param collation collation (sorting rule)
     */
    default void createDatabase(Connection connection, String databaseName, String charset, String collation) {
        throw new UnsupportedOperationException("Plugin does not support creating database");
    }

    /**
     * Check if a database exists
     * @param connection database connection
     * @param databaseName database name to check
     * @return true if database exists
     */
    default boolean databaseExists(Connection connection, String databaseName) {
        throw new UnsupportedOperationException("Plugin does not support checking database existence");
    }

    /**
     * Get list of available table engines
     * @param connection database connection
     * @return list of engine names
     */
    default List<String> getTableEngines(Connection connection) {
        throw new UnsupportedOperationException("Plugin does not support getting table engines");
    }

    /**
     * Create a new table
     * @param connection database connection
     * @param databaseName database name
     * @param tableName table name
     * @param columns column definitions
     * @param options table creation options (engine, charset, collation, comment, primaryKey, indexes, foreignKeys, constraints)
     */
    default void createTable(Connection connection, String databaseName, String tableName,
                            List<ColumnDefinition> columns, CreateTableOptions options) {
        throw new UnsupportedOperationException("Plugin does not support creating table");
    }

    /**
     * Create a new view
     * @param connection database connection
     * @param databaseName database name
     * @param viewName view name
     * @param query SELECT query for the view
     * @param options view creation options (algorithm, definer, sqlSecurity, checkOption, comment)
     */
    default void createView(Connection connection, String databaseName, String viewName,
                           String query, CreateViewOptions options) {
        throw new UnsupportedOperationException("Plugin does not support creating view");
    }

    /**
     * Create a new trigger
     * @param connection database connection
     * @param databaseName database name (catalog)
     * @param schemaName schema name
     * @param triggerName trigger name
     * @param tableName table name to associate with trigger
     * @param timing timing (BEFORE, AFTER)
     * @param event event type (INSERT, UPDATE, DELETE)
     * @param body trigger body (BEGIN...END)
     * @param options trigger creation options (definer, characteristics)
     */
    default void createTrigger(Connection connection, String databaseName, String schemaName, String triggerName,
                              String tableName, String timing, String event, String body, CreateTriggerOptions options) {
        throw new UnsupportedOperationException("Plugin does not support creating trigger");
    }

    /**
     * Create a new stored procedure
     * @param connection database connection
     * @param databaseName database name (catalog)
     * @param schemaName schema name
     * @param procedureName procedure name
     * @param parameters procedure parameters
     * @param body procedure body
     * @param options procedure creation options
     */
    default void createProcedure(Connection connection, String databaseName, String schemaName, String procedureName,
                                List<ParameterDefinition> parameters, String body, CreateRoutineOptions options) {
        throw new UnsupportedOperationException("Plugin does not support creating procedure");
    }

    /**
     * Create a new function
     * @param connection database connection
     * @param databaseName database name (catalog)
     * @param schemaName schema name
     * @param functionName function name
     * @param parameters function parameters
     * @param returnType return data type
     * @param body function body
     * @param options function creation options
     */
    default void createFunction(Connection connection, String databaseName, String schemaName, String functionName,
                               List<ParameterDefinition> parameters, String returnType, String body, CreateRoutineOptions options) {
        throw new UnsupportedOperationException("Plugin does not support creating function");
    }

    /**
     * Table creation options
     */
    class CreateTableOptions {
        private String engine;
        private String charset;
        private String collation;
        private String comment;
        private List<String> primaryKey;
        private List<IndexDefinition> indexes;
        private List<ForeignKeyDefinition> foreignKeys;
        private List<String> constraints;

        public CreateTableOptions() {}

        // Getters and setters
        public String getEngine() { return engine; }
        public void setEngine(String engine) { this.engine = engine; }
        public String getCharset() { return charset; }
        public void setCharset(String charset) { this.charset = charset; }
        public String getCollation() { return collation; }
        public void setCollation(String collation) { this.collation = collation; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public List<String> getPrimaryKey() { return primaryKey; }
        public void setPrimaryKey(List<String> primaryKey) { this.primaryKey = primaryKey; }
        public List<IndexDefinition> getIndexes() { return indexes; }
        public void setIndexes(List<IndexDefinition> indexes) { this.indexes = indexes; }
        public List<ForeignKeyDefinition> getForeignKeys() { return foreignKeys; }
        public void setForeignKeys(List<ForeignKeyDefinition> foreignKeys) { this.foreignKeys = foreignKeys; }
        public List<String> getConstraints() { return constraints; }
        public void setConstraints(List<String> constraints) { this.constraints = constraints; }
    }

    /**
     * Index definition for table creation
     */
    class IndexDefinition {
        private String name;
        private List<String> columns;
        private String type;

        public IndexDefinition() {}

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    /**
     * Foreign key definition for table creation
     */
    class ForeignKeyDefinition {
        private String name;
        private String column;
        private String referencedTable;
        private String referencedColumn;
        private String onDelete;
        private String onUpdate;

        public ForeignKeyDefinition() {}

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getColumn() { return column; }
        public void setColumn(String column) { this.column = column; }
        public String getReferencedTable() { return referencedTable; }
        public void setReferencedTable(String referencedTable) { this.referencedTable = referencedTable; }
        public String getReferencedColumn() { return referencedColumn; }
        public void setReferencedColumn(String referencedColumn) { this.referencedColumn = referencedColumn; }
        public String getOnDelete() { return onDelete; }
        public void setOnDelete(String onDelete) { this.onDelete = onDelete; }
        public String getOnUpdate() { return onUpdate; }
        public void setOnUpdate(String onUpdate) { this.onUpdate = onUpdate; }
    }

    /**
     * View creation options
     */
    class CreateViewOptions {
        private String algorithm;
        private String definer;
        private String sqlSecurity;
        private String checkOption;

        public CreateViewOptions() {}

        // Getters and setters
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public String getDefiner() { return definer; }
        public void setDefiner(String definer) { this.definer = definer; }
        public String getSqlSecurity() { return sqlSecurity; }
        public void setSqlSecurity(String sqlSecurity) { this.sqlSecurity = sqlSecurity; }
        public String getCheckOption() { return checkOption; }
        public void setCheckOption(String checkOption) { this.checkOption = checkOption; }
    }

    /**
     * Trigger creation options
     */
    class CreateTriggerOptions {
        private String definer;
        private String comment;
        private String sqlSecurity;

        public CreateTriggerOptions() {}

        // Getters and setters
        public String getDefiner() { return definer; }
        public void setDefiner(String definer) { this.definer = definer; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public String getSqlSecurity() { return sqlSecurity; }
        public void setSqlSecurity(String sqlSecurity) { this.sqlSecurity = sqlSecurity; }
    }

    /**
     * Stored procedure/function creation options
     */
    class CreateRoutineOptions {
        private String definer;
        private String comment;
        private String sqlSecurity;
        private String language;
        private String algorithm;

        public CreateRoutineOptions() {}

        // Getters and setters
        public String getDefiner() { return definer; }
        public void setDefiner(String definer) { this.definer = definer; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public String getSqlSecurity() { return sqlSecurity; }
        public void setSqlSecurity(String sqlSecurity) { this.sqlSecurity = sqlSecurity; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    }

    /**
     * Parameter definition for stored procedure/function
     */
    class ParameterDefinition {
        private String name;
        private String type;
        private String mode; // IN, OUT, INOUT

        public ParameterDefinition() {}

        public ParameterDefinition(String name, String type, String mode) {
            this.name = name;
            this.type = type;
            this.mode = mode;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
    }

    /**
     * Column definition for table creation
     */
    class ColumnDefinition {
        private String name;
        private String type;
        private Integer length;
        private Integer decimals;
        private boolean nullable;
        private String keyType; // PRI, UNI, MUL
        private String defaultValue;
        private String comment;
        private boolean autoIncrement;

        public ColumnDefinition() {}

        public ColumnDefinition(String name, String type) {
            this.name = name;
            this.type = type;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Integer getLength() { return length; }
        public void setLength(Integer length) { this.length = length; }
        public Integer getDecimals() { return decimals; }
        public void setDecimals(Integer decimals) { this.decimals = decimals; }
        public boolean isNullable() { return nullable; }
        public void setNullable(boolean nullable) { this.nullable = nullable; }
        public String getKeyType() { return keyType; }
        public void setKeyType(String keyType) { this.keyType = keyType; }
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public boolean isAutoIncrement() { return autoIncrement; }
        public void setAutoIncrement(boolean autoIncrement) { this.autoIncrement = autoIncrement; }
    }
}
