package edu.zsc.ai.plugin.mysql.value;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.zsc.ai.plugin.mysql.value.template.MySQLValueProcessorFactory;
import edu.zsc.ai.plugin.value.DefaultValueProcessor;

/**
 * MySQL-specific value processor that handles MySQL data type conversions.
 *
 * @author hhz
 */
public class MySQLValueProcessor extends DefaultValueProcessor {

    private static final Logger log = LoggerFactory.getLogger(MySQLValueProcessor.class);

    @Override
    public Object getJdbcValue(ResultSet resultSet, int columnIndex, int sqlType, String columnTypeName)
            throws SQLException {
        
        // First check if value is null
        Object value = resultSet.getObject(columnIndex);
        if (Objects.isNull(value)) {
            // MySQL special case: invalid dates like "0000-00-00"
            String stringValue = resultSet.getString(columnIndex);
            if (Objects.nonNull(stringValue)) {
                return stringValue;
            }
            return null;
        }

        // Handle empty strings
        if (value instanceof String emptyStr) {
            if (emptyStr.isEmpty()) {
                return emptyStr;
            }
        }

        // Delegate to type-specific processor via factory
        return convertJdbcValueByType(resultSet, columnIndex, columnTypeName);
    }

    @Override
    public Object convertJdbcValueByType(ResultSet resultSet, int columnIndex, String columnTypeName) 
            throws SQLException {
        try {
            // Try to get type-specific processor from factory (Chat2DB pattern)
            DefaultValueProcessor typeProcessor = MySQLValueProcessorFactory.getValueProcessor(columnTypeName);
            if (Objects.nonNull(typeProcessor)) {
                return typeProcessor.convertJdbcValueByType(resultSet, columnIndex, columnTypeName);
            }
        } catch (Exception e) {
            log.warn("Error using type-specific processor for type: {}", columnTypeName, e);
        }

        // Fallback to default conversion
        return super.convertJdbcValueByType(resultSet, columnIndex, columnTypeName);
    }
}
