package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.SchemaService;
import edu.zsc.ai.plugin.capability.SchemaProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.util.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaServiceImpl implements SchemaService {

    private final ConnectionService connectionService;

    @Override
    public List<String> listSchemas(Long connectionId, String catalog) {
        connectionService.openConnection(connectionId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getAnyOwnedActiveConnection(connectionId);

        SchemaProvider provider;
        try {
            provider = DefaultPluginManager.getInstance().getSchemaProviderByPluginId(active.pluginId());
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("Plugin does not support listing schemas: " + e.getMessage());
        }

        return provider.getSchemas(active.connection(), catalog);
    }
}
