package edu.zsc.ai.plugin.mysql.value.template;

import java.sql.ResultSet;
import java.sql.SQLException;

import edu.zsc.ai.plugin.value.DefaultValueProcessor;
import edu.zsc.ai.plugin.value.JdbcValueContext;

/**
 * MySQL INT type processor.
 * Handles INT, INTEGER types with UNSIGNED and ZEROFILL attributes.
 *
 * @author hhz
 * @date 2025-11-14
 */
public class MySQLIntProcessor extends DefaultValueProcessor {

    @Override
    public Object convertJdbcValueByType(JdbcValueContext context) throws SQLException {
        ResultSet resultSet = context.getResultSet();
        int columnIndex = context.getColumnIndex();
        String columnTypeName = context.getColumnTypeName();
        
        int value = resultSet.getInt(columnIndex);
        if (resultSet.wasNull()) {
            return null;
        }
        
        // Handle UNSIGNED: MySQL INT UNSIGNED range is 0 to 4294967295
        // JDBC getInt() returns signed int, so large unsigned values may be negative
        if (MySQLValueProcessorFactory.isUnsigned(columnTypeName) && value < 0) {
            // Convert to long for unsigned representation
            return Integer.toUnsignedLong(value);
        }
        
        return value;
    }
}
