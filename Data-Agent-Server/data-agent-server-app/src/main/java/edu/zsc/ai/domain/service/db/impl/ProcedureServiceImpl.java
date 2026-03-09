package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.ProcedureService;
import edu.zsc.ai.plugin.capability.ProcedureProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.model.metadata.ProcedureMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcedureServiceImpl implements ProcedureService {

    private final ConnectionService connectionService;

    @Override
    public List<ProcedureMetadata> getProcedures(DbContext db) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        ProcedureProvider provider = DefaultPluginManager.getInstance().getProcedureProviderByPluginId(active.pluginId());
        return provider.getProcedures(active.connection(), db.catalog(), db.schema());
    }

    @Override
    public List<ProcedureMetadata> searchProcedures(DbContext db, String procedureNamePattern) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        ProcedureProvider provider = DefaultPluginManager.getInstance().getProcedureProviderByPluginId(active.pluginId());
        return provider.searchProcedures(active.connection(), db.catalog(), db.schema(), procedureNamePattern);
    }

    @Override
    public long countProcedures(DbContext db, String procedureNamePattern) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        ProcedureProvider provider = DefaultPluginManager.getInstance().getProcedureProviderByPluginId(active.pluginId());
        return provider.countProcedures(active.connection(), db.catalog(), db.schema(), procedureNamePattern);
    }

    @Override
    public String getProcedureDdl(DbContext db, String procedureName) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        ProcedureProvider provider = DefaultPluginManager.getInstance().getProcedureProviderByPluginId(active.pluginId());
        return provider.getProcedureDdl(active.connection(), db.catalog(), db.schema(), procedureName);
    }

    @Override
    public void deleteProcedure(DbContext db, String procedureName) {
        connectionService.openConnection(db);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(db);

        ProcedureProvider provider = DefaultPluginManager.getInstance().getProcedureProviderByPluginId(active.pluginId());
        provider.deleteProcedure(active.connection(), db.catalog(), db.schema(), procedureName);

        log.info("Procedure deleted successfully: connectionId={}, catalog={}, schema={}, procedureName={}",
                db.connectionId(), db.catalog(), db.schema(), procedureName);
    }
}
