package edu.zsc.ai.plugin.mysql.value.template;

import java.sql.ResultSet;
import java.sql.SQLException;

import edu.zsc.ai.plugin.value.DefaultValueProcessor;
import edu.zsc.ai.plugin.value.JdbcValueContext;

/**
 * Processor for MySQL TIME type.
 * Converts to LocalTime string representation.
 *
 * @author hhz
 * @date 2025-11-15
 */
public class MySQLTimeProcessor extends DefaultValueProcessor {
    @Override
    public Object convertJdbcValueByType(JdbcValueContext context) throws SQLException {
        ResultSet resultSet = context.getResultSet();
        int columnIndex = context.getColumnIndex();
        java.sql.Time time = resultSet.getTime(columnIndex);
        return time != null ? time.toLocalTime().toString() : null;
    }
}
