package edu.zsc.ai.plugin.mysql.manager;

import edu.zsc.ai.plugin.capability.MysqlIdentifierEscaper;
import edu.zsc.ai.plugin.capability.TriggerManager;
import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.model.metadata.TriggerMetadata;
import edu.zsc.ai.plugin.mysql.constant.MySqlTemplate;
import edu.zsc.ai.plugin.mysql.constant.MysqlShowColumnConstants;
import edu.zsc.ai.plugin.mysql.constant.MysqlTriggerConstants;
import edu.zsc.ai.plugin.mysql.support.MysqlCapabilitySupport;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MysqlTriggerManager implements TriggerManager {

    private final MysqlCapabilitySupport support;

    public MysqlTriggerManager(MysqlCapabilitySupport support) {
        this.support = Objects.requireNonNull(support, "support");
    }

    @Override
    public List<TriggerMetadata> getTriggers(Connection connection, String catalog, String schema, String tableName) {
        if (connection == null) {
            return List.of();
        }

        String database = support.resolveDatabase(catalog, schema);
        if (StringUtils.isBlank(database)) {
            return List.of();
        }

        String escapedDatabase = MysqlIdentifierEscaper.getInstance().escapeStringLiteral(database);
        String sql = String.format(MySqlTemplate.SQL_LIST_TRIGGERS, escapedDatabase);
        if (StringUtils.isNotBlank(tableName)) {
            String escapedTable = MysqlIdentifierEscaper.getInstance().escapeStringLiteral(tableName);
            sql += MySqlTemplate.SQL_TRIGGER_FILTER_BY_TABLE + escapedTable + "'";
        }

        SqlCommandResult result = support.execute(connection, database, sql);
        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to list triggers: " + result.getErrorMessage());
        }

        List<TriggerMetadata> triggers = new ArrayList<>();
        if (result.getRows() == null) {
            return triggers;
        }

        for (List<Object> row : result.getRows()) {
            Object nameObject = result.getValueByColumnName(row, MysqlTriggerConstants.TRIGGER_NAME);
            Object tableObject = result.getValueByColumnName(row, MysqlTriggerConstants.EVENT_OBJECT_TABLE);
            Object timingObject = result.getValueByColumnName(row, MysqlTriggerConstants.ACTION_TIMING);
            Object eventObject = result.getValueByColumnName(row, MysqlTriggerConstants.EVENT_MANIPULATION);

            String name = nameObject != null ? nameObject.toString() : "";
            String table = tableObject != null ? tableObject.toString() : "";
            String timing = timingObject != null ? timingObject.toString() : "";
            String event = eventObject != null ? eventObject.toString() : "";

            if (StringUtils.isNotBlank(name)) {
                triggers.add(new TriggerMetadata(name, table, timing, event));
            }
        }
        return triggers;
    }

    @Override
    public String getTriggerDdl(Connection connection, String catalog, String schema, String triggerName) {
        return support.getObjectDdl(
                connection,
                catalog,
                triggerName,
                MySqlTemplate.SQL_SHOW_CREATE_TRIGGER,
                MysqlShowColumnConstants.SQL_ORIGINAL_STATEMENT,
                DatabaseObjectTypeEnum.TRIGGER.getValue()
        );
    }

    @Override
    public void deleteTrigger(Connection connection, String catalog, String schema, String triggerName) {
        support.dropObject(
                connection,
                catalog,
                triggerName,
                MySqlTemplate.SQL_DROP_TRIGGER,
                DatabaseObjectTypeEnum.TRIGGER.getValue(),
                true
        );
    }
}
