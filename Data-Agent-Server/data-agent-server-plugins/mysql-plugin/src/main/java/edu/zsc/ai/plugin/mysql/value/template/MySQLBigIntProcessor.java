package edu.zsc.ai.plugin.mysql.value.template;

import java.sql.ResultSet;
import java.sql.SQLException;

import edu.zsc.ai.plugin.value.DefaultValueProcessor;
import edu.zsc.ai.plugin.value.JdbcValueContext;

/**
 * Processor for MySQL BIGINT type.
 * Handles UNSIGNED BIGINT by converting to unsigned string representation.
 *
 * @author hhz
 * @date 2025-11-15
 */
public class MySQLBigIntProcessor extends DefaultValueProcessor {
    @Override
    public Object convertJdbcValueByType(JdbcValueContext context) throws SQLException {
        ResultSet resultSet = context.getResultSet();
        int columnIndex = context.getColumnIndex();
        String columnTypeName = context.getColumnTypeName();
        
        long value = resultSet.getLong(columnIndex);
        if (resultSet.wasNull()) {
            return null;
        }
        
        // Handle UNSIGNED BIGINT
        if (MySQLValueProcessorFactory.isUnsigned(columnTypeName) && value < 0) {
            // For unsigned bigint, we need to use BigInteger
            return Long.toUnsignedString(value);
        }
        
        return value;
    }
}
