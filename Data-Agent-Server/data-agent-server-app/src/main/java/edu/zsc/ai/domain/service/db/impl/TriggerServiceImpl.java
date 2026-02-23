package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.TriggerService;
import edu.zsc.ai.plugin.capability.TriggerProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
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

    @Override
    public List<TriggerMetadata> getTriggers(Long connectionId, String catalog, String schema, String tableName, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        TriggerProvider provider = DefaultPluginManager.getInstance().getTriggerProviderByPluginId(active.pluginId());
        return provider.getTriggers(active.connection(), catalog, schema, tableName);
    }

    @Override
    public String getTriggerDdl(Long connectionId, String catalog, String schema, String triggerName, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        TriggerProvider provider = DefaultPluginManager.getInstance().getTriggerProviderByPluginId(active.pluginId());
        return provider.getTriggerDdl(active.connection(), catalog, schema, triggerName);
    }

    @Override
    public void deleteTrigger(Long connectionId, String catalog, String schema, String triggerName, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        TriggerProvider provider = DefaultPluginManager.getInstance().getTriggerProviderByPluginId(active.pluginId());
        provider.deleteTrigger(active.connection(), catalog, schema, triggerName);

        log.info("Trigger deleted successfully: connectionId={}, catalog={}, schema={}, triggerName={}",
                connectionId, catalog, schema, triggerName);
    }
}
