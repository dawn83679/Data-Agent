package edu.zsc.ai.plugin.mysql.value.template;

import java.sql.SQLException;

import edu.zsc.ai.plugin.value.DefaultValueProcessor;
import edu.zsc.ai.plugin.value.JdbcValueContext;

/**
 * Processor for MySQL TEXT types (TEXT, TINYTEXT, MEDIUMTEXT, LONGTEXT).
 *
 * @author hhz
 * @date 2025-11-15
 */
public class MySQLTextProcessor extends DefaultValueProcessor {
    @Override
    public Object convertJdbcValueByType(JdbcValueContext context) throws SQLException {
        return context.getResultSet().getString(context.getColumnIndex());
    }
}
