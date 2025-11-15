package edu.zsc.ai.plugin.mysql.value.template;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.zsc.ai.plugin.mysql.value.template.MySQLCommonProcessors.MySQLBigIntProcessor;
import edu.zsc.ai.plugin.mysql.value.template.MySQLCommonProcessors.MySQLBinaryProcessor;
import edu.zsc.ai.plugin.mysql.value.template.MySQLCommonProcessors.MySQLBitProcessor;
import edu.zsc.ai.plugin.mysql.value.template.MySQLCommonProcessors.MySQLBlobProcessor;
import edu.zsc.ai.plugin.mysql.value.template.MySQLCommonProcessors.MySQLBooleanProcessor;
import edu.zsc.ai.plugin.mysql.value.template.MySQLCommonProcessors.MySQLDateTimeProcessor;
import edu.zsc.ai.plugin.mysql.value.template.MySQLCommonProcessors.MySQLDecimalProcessor;
import edu.zsc.ai.plugin.mysql.value.template.MySQLCommonProcessors.MySQLDoubleProcessor;
import edu.zsc.ai.plugin.mysql.value.template.MySQLCommonProcessors.MySQLEnumProcessor;
import edu.zsc.ai.plugin.mysql.value.template.MySQLCommonProcessors.MySQLFloatProcessor;
import edu.zsc.ai.plugin.mysql.value.template.MySQLCommonProcessors.MySQLSetProcessor;
import edu.zsc.ai.plugin.mysql.value.template.MySQLCommonProcessors.MySQLSmallIntProcessor;
import edu.zsc.ai.plugin.mysql.value.template.MySQLCommonProcessors.MySQLStringProcessor;
import edu.zsc.ai.plugin.mysql.value.template.MySQLCommonProcessors.MySQLTextProcessor;
import edu.zsc.ai.plugin.mysql.value.template.MySQLCommonProcessors.MySQLTimeProcessor;
import edu.zsc.ai.plugin.mysql.value.template.MySQLCommonProcessors.MySQLTimestampProcessor;
import edu.zsc.ai.plugin.mysql.value.template.MySQLCommonProcessors.MySQLTinyIntProcessor;
import edu.zsc.ai.plugin.mysql.value.template.MySQLCommonProcessors.MySQLYearProcessor;
import edu.zsc.ai.plugin.value.DefaultValueProcessor;

/**
 * Factory for creating type-specific value processors for MySQL.
 *
 * <p>This factory returns specialized processors for different MySQL data types,
 * allowing fine-grained control over type conversion logic.
 *
 * @author hhz
 * @date 2025-11-14
 */
public class MySQLValueProcessorFactory {

    private static final Logger log = LoggerFactory.getLogger(MySQLValueProcessorFactory.class);

    /**
     * Cache of type-specific processors
     */
    private static final Map<String, DefaultValueProcessor> PROCESSOR_CACHE = new HashMap<>();

    static {
        // Register type-specific processors
        registerProcessor("TINYINT", new MySQLTinyIntProcessor());
        registerProcessor("SMALLINT", new MySQLSmallIntProcessor());
        registerProcessor("INT", new MySQLIntProcessor());
        registerProcessor("INTEGER", new MySQLIntProcessor());
        registerProcessor("BIGINT", new MySQLBigIntProcessor());
        
        registerProcessor("FLOAT", new MySQLFloatProcessor());
        registerProcessor("DOUBLE", new MySQLDoubleProcessor());
        registerProcessor("DECIMAL", new MySQLDecimalProcessor());
        registerProcessor("NUMERIC", new MySQLDecimalProcessor());
        
        registerProcessor("DATE", new MySQLDateProcessor());
        registerProcessor("TIME", new MySQLTimeProcessor());
        registerProcessor("DATETIME", new MySQLDateTimeProcessor());
        registerProcessor("TIMESTAMP", new MySQLTimestampProcessor());
        registerProcessor("YEAR", new MySQLYearProcessor());
        
        registerProcessor("CHAR", new MySQLStringProcessor());
        registerProcessor("VARCHAR", new MySQLStringProcessor());
        registerProcessor("TEXT", new MySQLTextProcessor());
        registerProcessor("TINYTEXT", new MySQLTextProcessor());
        registerProcessor("MEDIUMTEXT", new MySQLTextProcessor());
        registerProcessor("LONGTEXT", new MySQLTextProcessor());
        
        registerProcessor("BINARY", new MySQLBinaryProcessor());
        registerProcessor("VARBINARY", new MySQLBinaryProcessor());
        registerProcessor("BLOB", new MySQLBlobProcessor());
        registerProcessor("TINYBLOB", new MySQLBlobProcessor());
        registerProcessor("MEDIUMBLOB", new MySQLBlobProcessor());
        registerProcessor("LONGBLOB", new MySQLBlobProcessor());
        
        registerProcessor("JSON", new MySQLJsonProcessor());
        registerProcessor("ENUM", new MySQLEnumProcessor());
        registerProcessor("SET", new MySQLSetProcessor());
        
        registerProcessor("BIT", new MySQLBitProcessor());
        registerProcessor("BOOLEAN", new MySQLBooleanProcessor());
        registerProcessor("BOOL", new MySQLBooleanProcessor());
    }

    /**
     * Register a type-specific processor.
     *
     * @param typeName the MySQL type name (case-insensitive)
     * @param processor the processor instance
     */
    private static void registerProcessor(String typeName, DefaultValueProcessor processor) {
        PROCESSOR_CACHE.put(typeName.toUpperCase(), processor);
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
        
        return PROCESSOR_CACHE.get(baseTypeName.toUpperCase());
    }

    /**
     * Extract base type name from full type definition.
     * Examples:
     * - "INT(11) UNSIGNED ZEROFILL" -> "INT"
     * - "VARCHAR(255)" -> "VARCHAR"
     * - "DECIMAL(10,2)" -> "DECIMAL"
     *
     * @param fullTypeName the full type name
     * @return the base type name
     */
    private static String extractBaseTypeName(String fullTypeName) {
        if (fullTypeName == null) {
            return "";
        }

        // Remove everything after first space or parenthesis
        int spaceIndex = fullTypeName.indexOf(' ');
        int parenIndex = fullTypeName.indexOf('(');
        
        int endIndex = fullTypeName.length();
        if (spaceIndex > 0) {
            endIndex = Math.min(endIndex, spaceIndex);
        }
        if (parenIndex > 0) {
            endIndex = Math.min(endIndex, parenIndex);
        }
        
        return fullTypeName.substring(0, endIndex).trim();
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

    /**
     * Check if a type has ZEROFILL attribute.
     *
     * @param fullTypeName the full type name
     * @return true if ZEROFILL
     */
    public static boolean isZeroFill(String fullTypeName) {
        return fullTypeName != null && fullTypeName.toUpperCase().contains("ZEROFILL");
    }
}
