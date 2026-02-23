package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.plugin.capability.DatabaseProvider.ColumnDefinition;
import edu.zsc.ai.plugin.capability.DatabaseProvider.CreateTableOptions;

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
}
