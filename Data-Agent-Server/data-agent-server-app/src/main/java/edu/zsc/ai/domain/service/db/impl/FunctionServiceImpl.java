package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.FunctionService;
import edu.zsc.ai.plugin.capability.FunctionProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.model.metadata.FunctionMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionServiceImpl implements FunctionService {

    private final ConnectionService connectionService;

    @Override
    public List<FunctionMetadata> getFunctions(DbContext db) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);
        FunctionProvider provider = DefaultPluginManager.getInstance().getFunctionProviderByPluginId(active.pluginId());
        try (ConnectionManager.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.getFunctions(borrowed.connection(), db.catalog(), db.schema());
        }
    }

    @Override
    public List<FunctionMetadata> searchFunctions(DbContext db, String functionNamePattern) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);
        FunctionProvider provider = DefaultPluginManager.getInstance().getFunctionProviderByPluginId(active.pluginId());
        try (ConnectionManager.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.searchFunctions(borrowed.connection(), db.catalog(), db.schema(), functionNamePattern);
        }
    }

    @Override
    public long countFunctions(DbContext db, String functionNamePattern) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);
        FunctionProvider provider = DefaultPluginManager.getInstance().getFunctionProviderByPluginId(active.pluginId());
        try (ConnectionManager.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.countFunctions(borrowed.connection(), db.catalog(), db.schema(), functionNamePattern);
        }
    }

    @Override
    public String getFunctionDdl(DbContext db, String functionName) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);
        FunctionProvider provider = DefaultPluginManager.getInstance().getFunctionProviderByPluginId(active.pluginId());
        try (ConnectionManager.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.getFunctionDdl(borrowed.connection(), db.catalog(), db.schema(), functionName);
        }
    }

    @Override
    public void deleteFunction(DbContext db, String functionName) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);
        FunctionProvider provider = DefaultPluginManager.getInstance().getFunctionProviderByPluginId(active.pluginId());
        try (ConnectionManager.BorrowedConnection borrowed = active.borrowConnection()) {
            provider.deleteFunction(borrowed.connection(), db.catalog(), db.schema(), functionName);
        }

        log.info("Function deleted successfully: connectionId={}, catalog={}, schema={}, functionName={}",
                db.connectionId(), db.catalog(), db.schema(), functionName);
    }
}
