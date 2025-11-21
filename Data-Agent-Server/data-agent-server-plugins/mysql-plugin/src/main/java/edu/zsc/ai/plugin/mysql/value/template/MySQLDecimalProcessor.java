package edu.zsc.ai.plugin.mysql.value.template;

import java.sql.ResultSet;
import java.sql.SQLException;

import edu.zsc.ai.plugin.value.DefaultValueProcessor;
import edu.zsc.ai.plugin.value.JdbcValueContext;

/**
 * Processor for MySQL DECIMAL/NUMERIC type.
 * Converts BigDecimal to string representation.
 *
 * @author hhz
 * @date 2025-11-15
 */
public class MySQLDecimalProcessor extends DefaultValueProcessor {
    @Override
    public Object convertJdbcValueByType(JdbcValueContext context) throws SQLException {
        ResultSet resultSet = context.getResultSet();
        int columnIndex = context.getColumnIndex();
        java.math.BigDecimal decimal = resultSet.getBigDecimal(columnIndex);
        return decimal != null ? decimal.toString() : null;
    }
}
