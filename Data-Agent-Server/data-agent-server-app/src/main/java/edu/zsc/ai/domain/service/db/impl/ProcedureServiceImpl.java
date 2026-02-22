package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.common.enums.db.DdlResourceTypeEnum;
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
    private final DdlFetcher ddlFetcher;

    @Override
    public List<ProcedureMetadata> listProcedures(Long connectionId, String catalog, String schema, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        ProcedureProvider provider = DefaultPluginManager.getInstance().getProcedureProviderByPluginId(active.pluginId());
        return provider.getProcedures(active.connection(), catalog, schema);
    }

    @Override
    public String getProcedureDdl(Long connectionId, String catalog, String schema, String procedureName, Long userId) {
        return ddlFetcher.fetch(connectionId, catalog, schema, procedureName, userId, DdlResourceTypeEnum.PROCEDURE,
                active -> {
                    ProcedureProvider provider = DefaultPluginManager.getInstance().getProcedureProviderByPluginId(active.pluginId());
                    return provider.getProcedureDdl(active.connection(), catalog, schema, procedureName);
                });
    }

    @Override
    public void deleteProcedure(Long connectionId, String catalog, String schema, String procedureName, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        ProcedureProvider provider = DefaultPluginManager.getInstance().getProcedureProviderByPluginId(active.pluginId());
        provider.deleteProcedure(active.connection(), active.pluginId(), catalog, schema, procedureName);

        log.info("Procedure deleted successfully: connectionId={}, catalog={}, schema={}, procedureName={}",
                connectionId, catalog, schema, procedureName);
    }
}
