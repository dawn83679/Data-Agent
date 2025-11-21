package edu.zsc.ai.plugin.mysql.value.template;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

import edu.zsc.ai.plugin.value.DefaultValueProcessor;
import edu.zsc.ai.plugin.value.JdbcValueContext;

/**
 * MySQL DATE type processor.
 * Handles MySQL-specific invalid dates like "0000-00-00".
 *
 * @author hhz
 * @date 2025-11-14
 */
public class MySQLDateProcessor extends DefaultValueProcessor {

    @Override
    public Object convertJdbcValueByType(JdbcValueContext context) throws SQLException {
        ResultSet resultSet = context.getResultSet();
        int columnIndex = context.getColumnIndex();
        
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
