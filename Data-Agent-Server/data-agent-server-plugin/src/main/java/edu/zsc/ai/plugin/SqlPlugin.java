package edu.zsc.ai.plugin;

/**
 * SQL database plugin marker interface.
 * Declares capability for database/catalog and schema concepts so that clients
 * (e.g. UI, metadata listing) can adapt. Default is true for both; override to false if not supported.
 */
public interface SqlPlugin extends Plugin {

    /**
     * Whether this engine supports the concept of database (catalog).
     * Default true; override to false if not supported.
     *
     * @return true if the engine has database/catalog concept
     */
    default boolean supportDatabase() {
        return true;
    }

    /**
     * Whether this engine supports schema as a separate namespace within a database.
     * Default true; override to false if not supported (e.g. MySQL where DATABASE and SCHEMA are synonyms).
     *
     * @return true if the engine has schema as a distinct concept from database
     */
    default boolean supportSchema() {
        return true;
    }
}
