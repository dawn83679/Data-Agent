package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.DatabaseService;
import edu.zsc.ai.plugin.capability.DatabaseProvider;
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
    public List<String> listDatabases(Long connectionId) {
        connectionService.openConnection(connectionId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getAnyOwnedActiveConnection(connectionId);

        DatabaseProvider provider = DefaultPluginManager.getInstance().getDatabaseProviderByPluginId(active.pluginId());

        return provider.getDatabases(active.connection());
    }
}
