package edu.zsc.ai.plugin.value;

/**
 * Abstract value context interface for different data source types.
 * This interface provides a unified abstraction for value extraction
 * from various data sources (JDBC, MongoDB, Redis, etc.).
 *
 * <p>Each data source type should provide its own implementation
 * with specific metadata and value access methods.
 *
 * @author hhz
 */
public interface ValueContext {

    /**
     * Get the data source type.
     *
     * @return the data source type (e.g., "JDBC", "MONGODB", "REDIS")
     */
    String getDataSourceType();

    /**
     * Get the column/field index.
     * For JDBC: column index (1-based)
     * For NoSQL: field index in document
     *
     * @return the index
     */
    int getIndex();

    /**
     * Get the column/field name.
     *
     * @return the name
     */
    String getName();

    /**
     * Get the data type name.
     * For JDBC: SQL type name (e.g., "VARCHAR", "INT")
     * For MongoDB: BSON type (e.g., "String", "ObjectId")
     * For Redis: value type (e.g., "STRING", "HASH")
     *
     * @return the type name
     */
    String getTypeName();

    /**
     * Get additional metadata as a generic object.
     * This allows each implementation to provide specific metadata.
     *
     * @param key the metadata key
     * @return the metadata value, or null if not found
     */
    Object getMetadata(String key);

    /**
     * Check if the value is nullable.
     *
     * @return true if nullable, false otherwise
     */
    default boolean isNullable() {
        return true;
    }
}
