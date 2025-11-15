package edu.zsc.ai.plugin.value;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default value processor that provides common type conversion logic.
 * Database-specific processors can extend this class and override specific methods.
 *
 *
 * @author hhz
 * @date 2025-11-14
 */
public class DefaultValueProcessor implements ValueProcessor {

    private static final Logger log = LoggerFactory.getLogger(DefaultValueProcessor.class);

    @Override
    public Object getJdbcValue(ResultSet resultSet, int columnIndex, int sqlType, String columnTypeName)
            throws SQLException {
        
        // Handle NULL values first
        Object value = resultSet.getObject(columnIndex);
        if (value == null) {
            return handleNullValue(resultSet, columnIndex, sqlType, columnTypeName);
        }

        // Handle empty strings
        if (value instanceof String && ((String) value).isEmpty()) {
            return value;
        }

        // Convert by type name
        return convertJdbcValueByType(resultSet, columnIndex, columnTypeName);
    }

    /**
     * Handle NULL values. Some databases may have special NULL representations.
     *
     * @param resultSet the ResultSet
     * @param columnIndex column index
     * @param sqlType SQL type
     * @param columnTypeName column type name
     * @return null or special value
     * @throws SQLException if error occurs
     */
    protected Object handleNullValue(ResultSet resultSet, int columnIndex, int sqlType, String columnTypeName)
            throws SQLException {
        // Check if there's a string representation for NULL (e.g., MySQL date "0000-00-00")
        String stringValue = resultSet.getString(columnIndex);
        if (stringValue != null && !stringValue.isEmpty()) {
            return stringValue;
        }
        return null;
    }

    /**
     * Convert JDBC value by type name.
     * This method should be overridden by subclasses to provide database-specific conversion.
     *
     * @param resultSet the ResultSet
     * @param columnIndex column index
     * @param columnTypeName column type name (e.g., "INT", "VARCHAR", "JSON")
     * @return converted value
     * @throws SQLException if error occurs
     */
    public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
            throws SQLException {
        // Default implementation: return as string
        return resultSet.getString(columnIndex);
    }

}
