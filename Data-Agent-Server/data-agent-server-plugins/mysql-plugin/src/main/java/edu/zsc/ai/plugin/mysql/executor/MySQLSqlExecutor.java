package edu.zsc.ai.plugin.mysql.executor;

import edu.zsc.ai.plugin.model.command.sql.AbstractSqlExecutor;
import edu.zsc.ai.plugin.mysql.value.MySQLValueProcessor;
import edu.zsc.ai.plugin.value.ValueProcessor;

/**
 * MySQL-specific SQL executor that handles MySQL data type conversions properly.
 * Uses MySQLValueProcessor for type-specific value extraction.
 *
 * <p>Handles special types like BLOB, CLOB, DATE, TIME, TIMESTAMP, JSON, etc.
 * through the factory-based value processor system.
 *
 * @author hhz
 */
public class MySQLSqlExecutor extends AbstractSqlExecutor {

    private final MySQLValueProcessor valueProcessor = new MySQLValueProcessor();

    @Override
    protected ValueProcessor getValueProcessor() {
        return valueProcessor;
    }
}
