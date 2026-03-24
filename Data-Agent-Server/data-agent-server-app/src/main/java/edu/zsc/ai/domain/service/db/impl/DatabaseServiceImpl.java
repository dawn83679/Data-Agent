package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.DatabaseService;
import edu.zsc.ai.plugin.capability.DatabaseManager;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseServiceImpl implements DatabaseService {

    private final ConnectionService connectionService;

    @Override
    public List<String> getDatabases(Long connectionId) {
        connectionService.openConnection(connectionId);

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getAnyOwnedActiveConnection(connectionId);
        DatabaseManager provider = DefaultPluginManager.getInstance().getDatabaseManagerByPluginId(active.pluginId());
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.getDatabases(borrowed.connection());
        }
    }

    @Override
    public void deleteDatabase(Long connectionId, String databaseName) {
        connectionService.openConnection(connectionId);

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getAnyOwnedActiveConnection(connectionId);
        DatabaseManager provider = DefaultPluginManager.getInstance().getDatabaseManagerByPluginId(active.pluginId());
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            provider.deleteDatabase(borrowed.connection(), databaseName);
        }

        log.info("Database deleted successfully: connectionId={}, databaseName={}", connectionId, databaseName);
    }
}
