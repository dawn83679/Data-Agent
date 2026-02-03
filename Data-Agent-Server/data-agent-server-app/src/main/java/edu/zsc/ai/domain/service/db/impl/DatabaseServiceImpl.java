package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.service.db.DatabaseService;
import edu.zsc.ai.plugin.capability.DatabaseProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.util.ConnectionPermissionChecker;
import edu.zsc.ai.util.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.List;

/**
 * Database Service Implementation
 * Lists databases on an active connection using plugin DatabaseProvider capability.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseServiceImpl implements DatabaseService {

    @Override
    public List<String> listDatabases(String connectionId) {
        ConnectionPermissionChecker.checkConnectionOwnership(connectionId);
        ConnectionManager.ConnectionMetadata metadata = ConnectionManager.getConnectionMetadata(connectionId);
        if (metadata == null) {
            throw BusinessException.notFound("Connection not found: " + connectionId);
        }

        Connection connection = ConnectionManager.getConnection(connectionId)
                .orElseThrow(() -> BusinessException.notFound("Connection not found: " + connectionId));

        DatabaseProvider provider;
        try {
            provider = DefaultPluginManager.getInstance().getDatabaseProviderByPluginId(metadata.pluginId());
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("Plugin does not support listing databases: " + e.getMessage());
        }

        return provider.getDatabases(connection);
    }
}
