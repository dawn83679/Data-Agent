package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.common.enums.db.DdlResourceTypeEnum;
import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.TriggerService;
import edu.zsc.ai.plugin.capability.CommandExecutor;
import edu.zsc.ai.plugin.capability.TriggerProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandRequest;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.model.metadata.TriggerMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriggerServiceImpl implements TriggerService {

    private final ConnectionService connectionService;
    private final DdlFetcher ddlFetcher;

    @Override
    public List<TriggerMetadata> listTriggers(Long connectionId, String catalog, String schema, String tableName, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        TriggerProvider provider = DefaultPluginManager.getInstance().getTriggerProviderByPluginId(active.pluginId());
        return provider.getTriggers(active.connection(), catalog, schema, tableName);
    }

    @Override
    public String getTriggerDdl(Long connectionId, String catalog, String schema, String triggerName, Long userId) {
        return ddlFetcher.fetch(connectionId, catalog, schema, triggerName, userId, DdlResourceTypeEnum.TRIGGER,
                active -> {
                    TriggerProvider provider = DefaultPluginManager.getInstance().getTriggerProviderByPluginId(active.pluginId());
                    return provider.getTriggerDdl(active.connection(), catalog, schema, triggerName);
                });
    }

    @Override
    public void deleteTrigger(Long connectionId, String catalog, String schema, String triggerName, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        CommandExecutor<SqlCommandRequest, SqlCommandResult> executor = DefaultPluginManager.getInstance()
                .getSqlCommandExecutorByPluginId(active.pluginId());

        String dropSql = buildDropTriggerSql(schema, triggerName);

        SqlCommandRequest pluginRequest = new SqlCommandRequest();
        pluginRequest.setConnection(active.connection());
        pluginRequest.setOriginalSql(dropSql);
        pluginRequest.setExecuteSql(dropSql);
        pluginRequest.setDatabase(catalog);
        pluginRequest.setSchema(schema);
        pluginRequest.setNeedTransaction(false);

        SqlCommandResult result = executor.executeCommand(pluginRequest);

        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to delete trigger: " + result.getErrorMessage());
        }

        log.info("Trigger deleted successfully: connectionId={}, catalog={}, schema={}, triggerName={}",
                connectionId, catalog, schema, triggerName);
    }

    private String buildDropTriggerSql(String schema, String triggerName) {
        // MySQL trigger name might contain table info like "triggerName(on table)"
        // We need to extract just the trigger name
        String cleanTriggerName = triggerName;
        int parenIndex = triggerName.indexOf('(');
        if (parenIndex > 0) {
            cleanTriggerName = triggerName.substring(0, parenIndex).trim();
        }

        StringBuilder sql = new StringBuilder("DROP TRIGGER IF EXISTS ");
        if (schema != null && !schema.isEmpty()) {
            sql.append("`").append(schema).append("`.");
        }
        sql.append("`").append(cleanTriggerName).append("`");
        return sql.toString();
    }
}
