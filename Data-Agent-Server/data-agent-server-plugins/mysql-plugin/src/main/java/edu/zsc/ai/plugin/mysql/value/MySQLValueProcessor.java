package edu.zsc.ai.plugin.mysql.value;

import edu.zsc.ai.plugin.mysql.value.template.MySQLValueProcessorFactory;
import edu.zsc.ai.plugin.value.DefaultValueProcessor;
import edu.zsc.ai.plugin.value.JdbcValueContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * MySQL-specific value processor that handles MySQL data type conversions.
 *
 * @author hhz
 */
public class MySQLValueProcessor extends DefaultValueProcessor {

    public static final MySQLValueProcessor INSTANCE = new MySQLValueProcessor();

    private MySQLValueProcessor (){

    }

    private static final Logger log = LoggerFactory.getLogger(MySQLValueProcessor.class);

    @Override
    public Object getJdbcValue(JdbcValueContext context) throws SQLException {
        ResultSet resultSet = context.getResultSet();
        int columnIndex = context.getColumnIndex();
        
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
        return convertJdbcValueByType(context);
    }

    @Override
    public Object convertJdbcValueByType(JdbcValueContext context) throws SQLException {
        try {
            // Try to get type-specific processor from factory
            DefaultValueProcessor typeProcessor = MySQLValueProcessorFactory.getValueProcessor(context.getColumnTypeName());
            if (Objects.nonNull(typeProcessor)) {
                return typeProcessor.convertJdbcValueByType(context);
            }
        } catch (Exception e) {
            log.warn("Error using type-specific processor for type: {}", context.getColumnTypeName(), e);
        }

        // Fallback to default conversion
        return super.convertJdbcValueByType(context);
    }
}
