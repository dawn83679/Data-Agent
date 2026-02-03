package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.service.db.SchemaService;
import edu.zsc.ai.plugin.capability.SchemaProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.util.ConnectionPermissionChecker;
import edu.zsc.ai.util.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.List;

/**
 * Schema Service Implementation
 * Lists schemas on an active connection using plugin SchemaProvider capability.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaServiceImpl implements SchemaService {

    @Override
    public List<String> listSchemas(String connectionId, String catalog) {
        ConnectionPermissionChecker.checkConnectionOwnership(connectionId);
        ConnectionManager.ConnectionMetadata metadata = ConnectionManager.getConnectionMetadata(connectionId);
        if (metadata == null) {
            throw BusinessException.notFound("Connection not found: " + connectionId);
        }

        Connection connection = ConnectionManager.getConnection(connectionId)
                .orElseThrow(() -> BusinessException.notFound("Connection not found: " + connectionId));

        SchemaProvider provider;
        try {
            provider = DefaultPluginManager.getInstance().getSchemaProviderByPluginId(metadata.pluginId());
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("Plugin does not support listing schemas: " + e.getMessage());
        }

        return provider.getSchemas(connection, catalog);
    }
}
