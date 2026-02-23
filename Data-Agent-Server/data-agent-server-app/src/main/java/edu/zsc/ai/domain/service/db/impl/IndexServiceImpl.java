package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.IndexService;
import edu.zsc.ai.plugin.capability.IndexProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.model.metadata.IndexMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService {

    private final ConnectionService connectionService;

    @Override
    public List<IndexMetadata> getIndexes(Long connectionId, String catalog, String schema, String tableName, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        IndexProvider provider = DefaultPluginManager.getInstance().getIndexProviderByPluginId(active.pluginId());
        return provider.getIndexes(active.connection(), catalog, schema, tableName);
    }
}
