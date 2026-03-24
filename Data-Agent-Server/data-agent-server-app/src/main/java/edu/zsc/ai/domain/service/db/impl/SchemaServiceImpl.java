package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.SchemaService;
import edu.zsc.ai.plugin.capability.SchemaManager;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.domain.exception.BusinessException;
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

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getAnyOwnedActiveConnection(connectionId);
        SchemaManager provider;
        try {
            provider = DefaultPluginManager.getInstance().getSchemaManagerByPluginId(active.pluginId());
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("Plugin does not support listing schemas: " + e.getMessage());
        }

        try (ActiveConnectionRegistry.BorrowedConnection borrowed = active.borrowConnection()) {
            return provider.getSchemas(borrowed.connection(), catalog);
        }
    }
}
