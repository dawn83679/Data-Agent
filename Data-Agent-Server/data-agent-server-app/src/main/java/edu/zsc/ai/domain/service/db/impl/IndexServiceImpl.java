package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.model.context.DbContext;
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
    public List<IndexMetadata> getIndexes(DbContext db, String tableName) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);
        IndexProvider provider = DefaultPluginManager.getInstance().getIndexProviderByPluginId(active.pluginId());
        try (ConnectionManager.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.getIndexes(borrowed.connection(), db.catalog(), db.schema(), tableName);
        }
    }
}
