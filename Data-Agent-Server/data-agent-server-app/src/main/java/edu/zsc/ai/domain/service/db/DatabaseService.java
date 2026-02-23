package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.plugin.capability.DatabaseProvider.ColumnDefinition;
import edu.zsc.ai.plugin.capability.DatabaseProvider.CreateRoutineOptions;
import edu.zsc.ai.plugin.capability.DatabaseProvider.CreateTableOptions;
import edu.zsc.ai.plugin.capability.DatabaseProvider.CreateTriggerOptions;
import edu.zsc.ai.plugin.capability.DatabaseProvider.CreateViewOptions;
import edu.zsc.ai.plugin.capability.DatabaseProvider.ParameterDefinition;

import java.util.List;

public interface DatabaseService {

    List<String> listDatabases(Long connectionId);

    /**
     * List databases for a connection, with explicit user for ownership. When userId is null, uses current login (StpUtil).
     */
    List<String> listDatabases(Long connectionId, Long userId);

    void deleteDatabase(Long connectionId, String databaseName, Long userId);

    /**
     * Get list of available character sets for a connection
     * @param connectionId connection id
     * @return list of character set names
     */
    List<String> getCharacterSets(Long connectionId);

    /**
     * Get list of available collations for a given character set
     * @param connectionId connection id
     * @param charset character set name
     * @return list of collation names
     */
    List<String> getCollations(Long connectionId, String charset);

    /**
     * Create a new database
     * @param connectionId connection id
     * @param databaseName database name
     * @param charset character set
     * @param collation collation (sorting rule)
     * @param userId user id
     */
    void createDatabase(Long connectionId, String databaseName, String charset, String collation, Long userId);

    /**
     * Check if a database exists
     * @param connectionId connection id
     * @param databaseName database name to check
     * @param userId user id
     * @return true if database exists
     */
    boolean databaseExists(Long connectionId, String databaseName, Long userId);

    /**
     * Get list of available table engines
     * @param connectionId connection id
     * @return list of engine names
     */
    List<String> getTableEngines(Long connectionId);

    /**
     * Create a new table
     * @param connectionId connection id
     * @param databaseName database name
     * @param tableName table name
     * @param columns column definitions
     * @param options table creation options (engine, charset, collation, comment, primaryKey, indexes, foreignKeys, constraints)
     * @param userId user id
     */
    void createTable(Long connectionId, String databaseName, String tableName,
                    List<ColumnDefinition> columns, CreateTableOptions options, Long userId);

    /**
     * Create a new view
     * @param connectionId connection id
     * @param databaseName database name
     * @param viewName view name
     * @param query SELECT query for the view
     * @param options view creation options (algorithm, definer, sqlSecurity, checkOption, comment)
     * @param userId user id
     */
    void createView(Long connectionId, String databaseName, String viewName,
                  String query, CreateViewOptions options, Long userId);

    /**
     * Create a new trigger
     * @param connectionId connection id
     * @param databaseName database name (catalog)
     * @param schemaName schema name
     * @param triggerName trigger name
     * @param tableName table name to associate with trigger
     * @param timing timing (BEFORE, AFTER)
     * @param event event type (INSERT, UPDATE, DELETE)
     * @param body trigger body (BEGIN...END)
     * @param options trigger creation options
     * @param userId user id
     */
    void createTrigger(Long connectionId, String databaseName, String schemaName, String triggerName,
                     String tableName, String timing, String event, String body,
                     CreateTriggerOptions options, Long userId);

    /**
     * Create a new stored procedure
     * @param connectionId connection id
     * @param databaseName database name (catalog)
     * @param schemaName schema name
     * @param procedureName procedure name
     * @param parameters procedure parameters
     * @param body procedure body
     * @param options procedure creation options
     * @param userId user id
     */
    void createProcedure(Long connectionId, String databaseName, String schemaName, String procedureName,
                        List<ParameterDefinition> parameters, String body,
                        CreateRoutineOptions options, Long userId);

    /**
     * Create a new function
     * @param connectionId connection id
     * @param databaseName database name (catalog)
     * @param schemaName schema name
     * @param functionName function name
     * @param parameters function parameters
     * @param returnType return data type
     * @param body function body
     * @param options function creation options
     * @param userId user id
     */
    void createFunction(Long connectionId, String databaseName, String schemaName, String functionName,
                       List<ParameterDefinition> parameters, String returnType, String body,
                       CreateRoutineOptions options, Long userId);
}
