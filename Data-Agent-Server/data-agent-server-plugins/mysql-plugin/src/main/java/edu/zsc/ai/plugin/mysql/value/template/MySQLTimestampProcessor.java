package edu.zsc.ai.plugin.mysql.value.template;

import java.sql.ResultSet;
import java.sql.SQLException;

import edu.zsc.ai.plugin.value.DefaultValueProcessor;
import edu.zsc.ai.plugin.value.JdbcValueContext;

/**
 * Processor for MySQL TIMESTAMP type.
 * Converts to LocalDateTime string representation.
 *
 * @author hhz
 * @date 2025-11-15
 */
public class MySQLTimestampProcessor extends DefaultValueProcessor {
    @Override
    public Object convertJdbcValueByType(JdbcValueContext context) throws SQLException {
        ResultSet resultSet = context.getResultSet();
        int columnIndex = context.getColumnIndex();
        java.sql.Timestamp timestamp = resultSet.getTimestamp(columnIndex);
        return timestamp != null ? timestamp.toLocalDateTime().toString() : null;
    }
}
