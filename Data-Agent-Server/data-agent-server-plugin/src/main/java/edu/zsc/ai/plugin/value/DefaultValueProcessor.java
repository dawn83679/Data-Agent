package edu.zsc.ai.plugin.value;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Default value processor that provides common type conversion logic.
 * Database-specific processors can extend this class and override specific methods.
 *
 * @author hhz
 * @date 2025-11-14
 */
public class DefaultValueProcessor implements ValueProcessor {

    @Override
    public Object getJdbcValue(JdbcValueContext context) throws SQLException {
        ResultSet resultSet = context.getResultSet();
        int columnIndex = context.getColumnIndex();
        
        // Handle NULL values first
        Object value = resultSet.getObject(columnIndex);
        if (value == null) {
            return handleNullValue(context);
        }

        // Handle empty strings
        if (value instanceof String emptyStr && emptyStr.isEmpty()) {
            return value;
        }

        // Convert by type name
        return convertJdbcValueByType(context);
    }

    /**
     * Handle NULL values. Some databases may have special NULL representations.
     *
     * @param context the JDBC value context
     * @return null or special value
     * @throws SQLException if error occurs
     */
    protected Object handleNullValue(JdbcValueContext context) throws SQLException {
        // Check if there's a string representation for NULL (e.g., MySQL date "0000-00-00")
        String stringValue = context.getResultSet().getString(context.getColumnIndex());
        if (stringValue != null && !stringValue.isEmpty()) {
            return stringValue;
        }
        return null;
    }

    /**
     * Convert JDBC value by type name.
     * This method can be overridden by subclasses to provide database-specific conversion.
     *
     * @param context the JDBC value context
     * @return converted value
     * @throws SQLException if error occurs
     */
    public Object convertJdbcValueByType(JdbcValueContext context) throws SQLException {
        // Default implementation: return as string
        return context.getResultSet().getString(context.getColumnIndex());
    }

    /**
     * Helper method to safely get value from ResultSet and check for NULL.
     *
     * @param resultSet the ResultSet
     * @param columnIndex the column index
     * @return the value, or null if SQL NULL
     * @throws SQLException if error occurs
     */
    protected Object getValueOrNull(ResultSet resultSet, int columnIndex) throws SQLException {
        Object value = resultSet.getObject(columnIndex);
        return resultSet.wasNull() ? null : value;
    }

    /**
     * Helper method to check if a value is null or empty string.
     *
     * @param value the value to check
     * @return true if null or empty string
     */
    protected boolean isNullOrEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String str) {
            return str.isEmpty();
        }
        return false;
    }
}
