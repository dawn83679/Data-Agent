package edu.zsc.ai.domain.service.db;

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
}
