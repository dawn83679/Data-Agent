package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.model.metadata.TriggerMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.List;

public interface TriggerProvider {

    /**
     * Escapes backticks in an identifier for MySQL by doubling them
     */
    default String escapeIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        return identifier.replace("`", "``");
    }

    List<TriggerMetadata> getTriggers(Connection connection, String catalog, String schema, String tableName);

    default String getTriggerDdl(Connection connection, String catalog, String schema, String triggerName) {
        throw new UnsupportedOperationException("Plugin does not support getting trigger DDL");
    }

    default void deleteTrigger(Connection connection, String pluginId, String catalog, String schema, String triggerName) {
        Logger log = LoggerFactory.getLogger(TriggerProvider.class);
        CommandExecutor<SqlCommandRequest, SqlCommandResult> executor = DefaultPluginManager.getInstance()
                .getSqlCommandExecutorByPluginId(pluginId);

        String dropSql = buildDropTriggerSql(schema, triggerName);

        SqlCommandRequest pluginRequest = new SqlCommandRequest();
        pluginRequest.setConnection(connection);
        pluginRequest.setOriginalSql(dropSql);
        pluginRequest.setExecuteSql(dropSql);
        pluginRequest.setDatabase(catalog);
        pluginRequest.setSchema(schema);
        pluginRequest.setNeedTransaction(false);

        SqlCommandResult result = executor.executeCommand(pluginRequest);

        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to delete trigger: " + result.getErrorMessage());
        }

        log.info("Trigger deleted successfully: catalog={}, schema={}, triggerName={}", catalog, schema, triggerName);
    }

    default String buildDropTriggerSql(String schema, String triggerName) {
        // MySQL trigger name might contain table info like "triggerName(on table)"
        // We need to extract just the trigger name
        String cleanTriggerName = triggerName;
        int parenIndex = triggerName.indexOf('(');
        if (parenIndex > 0) {
            cleanTriggerName = triggerName.substring(0, parenIndex).trim();
        }

        StringBuilder sql = new StringBuilder("DROP TRIGGER IF EXISTS ");
        if (schema != null && !schema.isEmpty()) {
            sql.append("`").append(escapeIdentifier(schema)).append("`.");
        }
        sql.append("`").append(escapeIdentifier(cleanTriggerName)).append("`");
        return sql.toString();
    }
}
