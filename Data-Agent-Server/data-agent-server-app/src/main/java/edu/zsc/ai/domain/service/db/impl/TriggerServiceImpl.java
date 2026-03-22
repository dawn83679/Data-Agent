package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.model.context.DbContext;
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
    public List<TriggerMetadata> getTriggers(DbContext db, String tableName) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);
        TriggerProvider provider = DefaultPluginManager.getInstance().getTriggerProviderByPluginId(active.pluginId());
        try (ConnectionManager.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.getTriggers(borrowed.connection(), db.catalog(), db.schema(), tableName);
        }
    }

    @Override
    public List<TriggerMetadata> searchTriggers(DbContext db, String tableName, String triggerNamePattern) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);
        TriggerProvider provider = DefaultPluginManager.getInstance().getTriggerProviderByPluginId(active.pluginId());
        try (ConnectionManager.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.searchTriggers(borrowed.connection(), db.catalog(), db.schema(), tableName, triggerNamePattern);
        }
    }

    @Override
    public String getTriggerDdl(DbContext db, String triggerName) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);
        TriggerProvider provider = DefaultPluginManager.getInstance().getTriggerProviderByPluginId(active.pluginId());
        try (ConnectionManager.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.getTriggerDdl(borrowed.connection(), db.catalog(), db.schema(), triggerName);
        }
    }

    @Override
    public void deleteTrigger(DbContext db, String triggerName) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);
        TriggerProvider provider = DefaultPluginManager.getInstance().getTriggerProviderByPluginId(active.pluginId());
        try (ConnectionManager.BorrowedConnection borrowed = active.borrowConnection()) {
            provider.deleteTrigger(borrowed.connection(), db.catalog(), db.schema(), triggerName);
        }

        log.info("Trigger deleted successfully: connectionId={}, catalog={}, schema={}, triggerName={}",
                db.connectionId(), db.catalog(), db.schema(), triggerName);
    }
}
