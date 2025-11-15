package edu.zsc.ai.plugin.mysql.value.template;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

import edu.zsc.ai.plugin.value.DefaultValueProcessor;

/**
 * MySQL DATE type processor.
 * Handles MySQL-specific invalid dates like "0000-00-00".
 *
 * @author hhz
 * @date 2025-11-14
 */
public class MySQLDateProcessor extends DefaultValueProcessor {

    @Override
    public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
            throws SQLException {
        try {
            Date date = resultSet.getDate(columnIndex);
            if (date != null) {
                return date.toLocalDate().toString();
            }
        } catch (SQLException e) {
            // MySQL may have invalid dates like "0000-00-00"
            // Try to get as string
            String stringValue = resultSet.getString(columnIndex);
            if (stringValue != null && !stringValue.isEmpty()) {
                return stringValue;
            }
        }
        return null;
    }
}
