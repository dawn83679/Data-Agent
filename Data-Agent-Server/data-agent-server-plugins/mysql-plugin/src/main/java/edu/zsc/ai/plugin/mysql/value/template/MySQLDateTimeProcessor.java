package edu.zsc.ai.plugin.mysql.value.template;

import java.sql.ResultSet;
import java.sql.SQLException;

import edu.zsc.ai.plugin.value.DefaultValueProcessor;
import edu.zsc.ai.plugin.value.JdbcValueContext;

/**
 * Processor for MySQL DATETIME type.
 * Handles invalid datetime values like "0000-00-00 00:00:00".
 *
 * @author hhz
 * @date 2025-11-15
 */
public class MySQLDateTimeProcessor extends DefaultValueProcessor {
    @Override
    public Object convertJdbcValueByType(JdbcValueContext context) throws SQLException {
        ResultSet resultSet = context.getResultSet();
        int columnIndex = context.getColumnIndex();
        
        try {
            java.sql.Timestamp timestamp = resultSet.getTimestamp(columnIndex);
            if (timestamp != null) {
                return timestamp.toLocalDateTime().toString();
            }
        } catch (SQLException e) {
            // MySQL may have invalid datetime like "0000-00-00 00:00:00"
            String stringValue = resultSet.getString(columnIndex);
            if (stringValue != null && !stringValue.isEmpty()) {
                return stringValue;
            }
        }
        return null;
    }
}
