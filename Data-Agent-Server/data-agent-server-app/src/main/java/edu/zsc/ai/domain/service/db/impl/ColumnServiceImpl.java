package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.service.db.ColumnService;
import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.plugin.capability.ColumnManager;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.model.metadata.ColumnMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ColumnServiceImpl implements ColumnService {

    private final ConnectionService connectionService;

    @Override
    public List<ColumnMetadata> listColumns(DbContext db, String tableName) {
        connectionService.openConnection(db);

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getOwnedConnection(db);
        ColumnManager provider = DefaultPluginManager.getInstance().getColumnManagerByPluginId(active.pluginId());
        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.getColumns(borrowed.connection(), db.catalog(), db.schema(), tableName);
        }
    }
}
