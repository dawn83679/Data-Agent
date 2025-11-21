package edu.zsc.ai.plugin.mysql.value;

/**
 * Enumeration of MySQL data types.
 * 
 * <p>This enum defines all standard MySQL data types and provides
 * utility methods for type identification and categorization.
 *
 * @author Data-Agent Team
 * @date 2025-11-15
 */
public enum MySQLDataType {
    // Integer types
    TINYINT("TINYINT", TypeCategory.INTEGER),
    SMALLINT("SMALLINT", TypeCategory.INTEGER),
    MEDIUMINT("MEDIUMINT", TypeCategory.INTEGER),
    INT("INT", TypeCategory.INTEGER),
    INTEGER("INTEGER", TypeCategory.INTEGER),
    BIGINT("BIGINT", TypeCategory.INTEGER),
    
    // Floating point types
    FLOAT("FLOAT", TypeCategory.FLOATING_POINT),
    DOUBLE("DOUBLE", TypeCategory.FLOATING_POINT),
    DECIMAL("DECIMAL", TypeCategory.FLOATING_POINT),
    NUMERIC("NUMERIC", TypeCategory.FLOATING_POINT),
    
    // Date and time types
    DATE("DATE", TypeCategory.TEMPORAL),
    TIME("TIME", TypeCategory.TEMPORAL),
    DATETIME("DATETIME", TypeCategory.TEMPORAL),
    TIMESTAMP("TIMESTAMP", TypeCategory.TEMPORAL),
    YEAR("YEAR", TypeCategory.TEMPORAL),
    
    // String types
    CHAR("CHAR", TypeCategory.STRING),
    VARCHAR("VARCHAR", TypeCategory.STRING),
    
    // Text types
    TEXT("TEXT", TypeCategory.TEXT),
    TINYTEXT("TINYTEXT", TypeCategory.TEXT),
    MEDIUMTEXT("MEDIUMTEXT", TypeCategory.TEXT),
    LONGTEXT("LONGTEXT", TypeCategory.TEXT),
    
    // Binary types
    BINARY("BINARY", TypeCategory.BINARY),
    VARBINARY("VARBINARY", TypeCategory.BINARY),
    
    // BLOB types (all variants handled by single BLOB type)
    BLOB("BLOB", TypeCategory.BLOB),
    
    // Special types
    JSON("JSON", TypeCategory.SPECIAL),
    ENUM("ENUM", TypeCategory.SPECIAL),
    SET("SET", TypeCategory.SPECIAL),
    BIT("BIT", TypeCategory.SPECIAL),
    BOOLEAN("BOOLEAN", TypeCategory.SPECIAL),
    BOOL("BOOL", TypeCategory.SPECIAL);
    
    private final String typeName;
    private final TypeCategory category;
    
    MySQLDataType(String typeName, TypeCategory category) {
        this.typeName = typeName;
        this.category = category;
    }
    
    public String getTypeName() {
        return typeName;
    }
    
    public TypeCategory getCategory() {
        return category;
    }
    
    /**
     * Get MySQLDataType from type name string.
     * 
     * @param typeName the type name (case-insensitive)
     * @return the corresponding MySQLDataType, or null if not found
     */
    public static MySQLDataType fromTypeName(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return null;
        }
        
        String upperTypeName = typeName.toUpperCase();
        
        // Handle BLOB variants (TINYBLOB, MEDIUMBLOB, LONGBLOB -> BLOB)
        if (upperTypeName.endsWith("BLOB")) {
            return BLOB;
        }
        
        for (MySQLDataType type : values()) {
            if (type.typeName.equals(upperTypeName)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * Check if this type is an integer type.
     */
    public boolean isInteger() {
        return category == TypeCategory.INTEGER;
    }
    
    /**
     * Check if this type is a floating point type.
     */
    public boolean isFloatingPoint() {
        return category == TypeCategory.FLOATING_POINT;
    }
    
    /**
     * Check if this type is a temporal (date/time) type.
     */
    public boolean isTemporal() {
        return category == TypeCategory.TEMPORAL;
    }
    
    /**
     * Check if this type is a string type.
     */
    public boolean isString() {
        return category == TypeCategory.STRING;
    }
    
    /**
     * Check if this type is a text type.
     */
    public boolean isText() {
        return category == TypeCategory.TEXT;
    }
    
    /**
     * Check if this type is a binary type.
     */
    public boolean isBinary() {
        return category == TypeCategory.BINARY;
    }
    
    /**
     * Check if this type is a BLOB type.
     */
    public boolean isBlob() {
        return category == TypeCategory.BLOB;
    }
    
    /**
     * Category of MySQL data types.
     */
    public enum TypeCategory {
        INTEGER,
        FLOATING_POINT,
        TEMPORAL,
        STRING,
        TEXT,
        BINARY,
        BLOB,
        SPECIAL
    }
}
