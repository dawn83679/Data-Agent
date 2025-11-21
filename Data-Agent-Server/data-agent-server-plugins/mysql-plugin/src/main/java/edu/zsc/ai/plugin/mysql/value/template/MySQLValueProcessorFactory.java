package edu.zsc.ai.plugin.mysql.value.template;

import edu.zsc.ai.plugin.mysql.value.MySQLDataType;
import edu.zsc.ai.plugin.value.DefaultValueProcessor;

import java.util.EnumMap;
import java.util.Map;

/**
 * Factory for creating type-specific value processors for MySQL.
 *
 * <p>This factory uses enum-based type mapping and shared processor instances
 * for better performance and memory efficiency.
 *
 * @author hhz
 * @date 2025-11-14
 */
public class MySQLValueProcessorFactory {

    /**
     * Shared processor instances (one instance per processor type)
     */
    private static final MySQLTinyIntProcessor TINYINT_PROCESSOR = new MySQLTinyIntProcessor();
    private static final MySQLSmallIntProcessor SMALLINT_PROCESSOR = new MySQLSmallIntProcessor();
    private static final MySQLIntProcessor INT_PROCESSOR = new MySQLIntProcessor();
    private static final MySQLBigIntProcessor BIGINT_PROCESSOR = new MySQLBigIntProcessor();
    private static final MySQLFloatProcessor FLOAT_PROCESSOR = new MySQLFloatProcessor();
    private static final MySQLDoubleProcessor DOUBLE_PROCESSOR = new MySQLDoubleProcessor();
    private static final MySQLDecimalProcessor DECIMAL_PROCESSOR = new MySQLDecimalProcessor();
    private static final MySQLDateProcessor DATE_PROCESSOR = new MySQLDateProcessor();
    private static final MySQLTimeProcessor TIME_PROCESSOR = new MySQLTimeProcessor();
    private static final MySQLDateTimeProcessor DATETIME_PROCESSOR = new MySQLDateTimeProcessor();
    private static final MySQLTimestampProcessor TIMESTAMP_PROCESSOR = new MySQLTimestampProcessor();
    private static final MySQLYearProcessor YEAR_PROCESSOR = new MySQLYearProcessor();
    private static final MySQLTextProcessor TEXT_PROCESSOR = new MySQLTextProcessor();
    private static final MySQLBinaryProcessor BINARY_PROCESSOR = new MySQLBinaryProcessor();
    private static final MySQLBlobProcessor BLOB_PROCESSOR = new MySQLBlobProcessor();
    private static final MySQLBitProcessor BIT_PROCESSOR = new MySQLBitProcessor();
    private static final MySQLBooleanProcessor BOOLEAN_PROCESSOR = new MySQLBooleanProcessor();

    /**
     * Enum-based processor cache for fast lookup
     */
    private static final Map<MySQLDataType, DefaultValueProcessor> PROCESSOR_MAP = new EnumMap<>(MySQLDataType.class);

    static {
        // Register shared processor instances using enum keys
        PROCESSOR_MAP.put(MySQLDataType.TINYINT, TINYINT_PROCESSOR);
        PROCESSOR_MAP.put(MySQLDataType.SMALLINT, SMALLINT_PROCESSOR);
        PROCESSOR_MAP.put(MySQLDataType.INT, INT_PROCESSOR);
        PROCESSOR_MAP.put(MySQLDataType.INTEGER, INT_PROCESSOR);  // Alias
        PROCESSOR_MAP.put(MySQLDataType.BIGINT, BIGINT_PROCESSOR);
        
        PROCESSOR_MAP.put(MySQLDataType.FLOAT, FLOAT_PROCESSOR);
        PROCESSOR_MAP.put(MySQLDataType.DOUBLE, DOUBLE_PROCESSOR);
        PROCESSOR_MAP.put(MySQLDataType.DECIMAL, DECIMAL_PROCESSOR);
        PROCESSOR_MAP.put(MySQLDataType.NUMERIC, DECIMAL_PROCESSOR);  // Alias
        
        PROCESSOR_MAP.put(MySQLDataType.DATE, DATE_PROCESSOR);
        PROCESSOR_MAP.put(MySQLDataType.TIME, TIME_PROCESSOR);
        PROCESSOR_MAP.put(MySQLDataType.DATETIME, DATETIME_PROCESSOR);
        PROCESSOR_MAP.put(MySQLDataType.TIMESTAMP, TIMESTAMP_PROCESSOR);
        PROCESSOR_MAP.put(MySQLDataType.YEAR, YEAR_PROCESSOR);
        

        PROCESSOR_MAP.put(MySQLDataType.TEXT, TEXT_PROCESSOR);
        PROCESSOR_MAP.put(MySQLDataType.TINYTEXT, TEXT_PROCESSOR);
        PROCESSOR_MAP.put(MySQLDataType.MEDIUMTEXT, TEXT_PROCESSOR);
        PROCESSOR_MAP.put(MySQLDataType.LONGTEXT, TEXT_PROCESSOR);
        
        PROCESSOR_MAP.put(MySQLDataType.BINARY, BINARY_PROCESSOR);
        PROCESSOR_MAP.put(MySQLDataType.VARBINARY, BINARY_PROCESSOR);
        
        // BLOB type (all BLOB variants are handled by MySQLDataType.BLOB)
        PROCESSOR_MAP.put(MySQLDataType.BLOB, BLOB_PROCESSOR);
        
        PROCESSOR_MAP.put(MySQLDataType.BIT, BIT_PROCESSOR);
        PROCESSOR_MAP.put(MySQLDataType.BOOLEAN, BOOLEAN_PROCESSOR);
        PROCESSOR_MAP.put(MySQLDataType.BOOL, BOOLEAN_PROCESSOR);  // Alias
    }

    /**
     * Get a type-specific processor for the given MySQL type name.
     *
     * @param columnTypeName the MySQL column type name (e.g., "INT", "VARCHAR", "DATETIME")
     * @return the processor, or null if no specific processor is registered
     */
    public static DefaultValueProcessor getValueProcessor(String columnTypeName) {
        if (columnTypeName == null || columnTypeName.isEmpty()) {
            return null;
        }

        // Extract base type name (remove size, unsigned, zerofill, etc.)
        String baseTypeName = extractBaseTypeName(columnTypeName);
        
        // Convert to enum and lookup processor
        MySQLDataType dataType = MySQLDataType.fromTypeName(baseTypeName);
        return dataType != null ? PROCESSOR_MAP.get(dataType) : null;
    }

    /**
     * Extract base type name from full type definition.
     * For MySQL 8.0+, getColumnTypeName() returns clean type names without display width.
     * This method only needs to remove attributes like UNSIGNED.
     * 
     * Examples:
     * - "INT UNSIGNED" -> "INT"
     * - "BIGINT UNSIGNED" -> "BIGINT"
     * - "VARCHAR" -> "VARCHAR"
     * - "DECIMAL" -> "DECIMAL"
     *
     * @param fullTypeName the full type name
     * @return the base type name
     */
    private static String extractBaseTypeName(String fullTypeName) {
        if (fullTypeName == null || fullTypeName.isEmpty()) {
            return "";
        }

        // Remove attributes after space (UNSIGNED, ZEROFILL, etc.)
        int spaceIndex = fullTypeName.indexOf(' ');
        if (spaceIndex > 0) {
            return fullTypeName.substring(0, spaceIndex).trim();
        }
        
        return fullTypeName.trim();
    }

    /**
     * Check if a type has UNSIGNED attribute.
     *
     * @param fullTypeName the full type name
     * @return true if UNSIGNED
     */
    public static boolean isUnsigned(String fullTypeName) {
        return fullTypeName != null && fullTypeName.toUpperCase().contains("UNSIGNED");
    }


}
