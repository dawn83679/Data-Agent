package edu.zsc.ai.plugin.mysql.value.template;

import java.sql.ResultSet;
import java.sql.SQLException;

import edu.zsc.ai.plugin.value.DefaultValueProcessor;
import edu.zsc.ai.plugin.value.JdbcValueContext;

/**
 * Processor for MySQL YEAR type.
 *
 * @author hhz
 * @date 2025-11-15
 */
public class MySQLYearProcessor extends DefaultValueProcessor {
    @Override
    public Object convertJdbcValueByType(JdbcValueContext context) throws SQLException {
        ResultSet resultSet = context.getResultSet();
        int columnIndex = context.getColumnIndex();
        int value = resultSet.getInt(columnIndex);
        return resultSet.wasNull() ? null : value;
    }
}
