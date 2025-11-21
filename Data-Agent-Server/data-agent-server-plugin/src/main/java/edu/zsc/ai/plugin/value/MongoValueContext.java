package edu.zsc.ai.plugin.value;

import java.util.HashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * MongoDB-specific implementation of ValueContext.
 * Encapsulates all parameters needed for MongoDB document field processing.
 *
 * <p>This class is specific to MongoDB data sources and provides
 * MongoDB-specific metadata like BSON type, document reference, etc.
 *
 * @author hhz
 */
@Data
@Builder
public class MongoValueContext implements ValueContext {

    /**
     * The MongoDB document (org.bson.Document)
     * Using Object to avoid direct dependency on MongoDB driver
     */
    private Object document;

    /**
     * The field name in the document
     */
    private String fieldName;

    /**
     * The field index (for ordered access)
     */
    private int fieldIndex;

    /**
     * The BSON type name (e.g., "String", "ObjectId", "Date", "Array")
     */
    private String bsonTypeName;

    /**
     * Whether the field is nullable
     */
    @Builder.Default
    private Boolean nullable = true;

    /**
     * Additional metadata storage
     */
    @Builder.Default
    private Map<String, Object> additionalMetadata = new HashMap<>();

    // ==================== ValueContext Interface Implementation ====================

    @Override
    public String getDataSourceType() {
        return "MONGODB";
    }

    @Override
    public int getIndex() {
        return fieldIndex;
    }

    @Override
    public String getName() {
        return fieldName;
    }

    @Override
    public String getTypeName() {
        return bsonTypeName;
    }

    @Override
    public Object getMetadata(String key) {
        return additionalMetadata.get(key);
    }

    @Override
    public boolean isNullable() {
        return nullable != null ? nullable : true;
    }

    /**
     * Add additional metadata.
     *
     * @param key the metadata key
     * @param value the metadata value
     */
    public void addMetadata(String key, Object value) {
        additionalMetadata.put(key, value);
    }
}
