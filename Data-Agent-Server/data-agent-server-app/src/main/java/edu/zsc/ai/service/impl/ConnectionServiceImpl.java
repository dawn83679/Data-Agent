package edu.zsc.ai.service.impl;

import edu.zsc.ai.exception.BusinessException;
import edu.zsc.ai.model.dto.request.ConnectRequest;
import edu.zsc.ai.model.dto.response.ConnectionTestResponse;
import edu.zsc.ai.model.enums.ConnectionTestStatus;
import edu.zsc.ai.plugin.Plugin;
import edu.zsc.ai.plugin.capability.ConnectionProvider;
import edu.zsc.ai.plugin.manager.PluginManager;
import edu.zsc.ai.plugin.model.ConnectionConfig;
import edu.zsc.ai.service.ConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Connection Service Implementation
 * Manages database connections using plugin system.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionServiceImpl implements ConnectionService {

    private final PluginManager pluginManager;

    /**
     * Active connections registry: connectionId -> Connection
     */
    private final Map<String, Connection> activeConnections = new ConcurrentHashMap<>();

    /**
     * Connection metadata registry: connectionId -> ConnectionMetadata
     */
    private final Map<String, ConnectionMetadata> connectionMetadata = new ConcurrentHashMap<>();

    @Override
    public ConnectionTestResponse testConnection(ConnectRequest request) {
        Connection connection = null;
        long startTime = System.currentTimeMillis();
        // Get any available plugin for the database type
        ConnectionProvider provider = getConnectionProviderByDbType(request.getDbType());
        // Convert request to ConnectionConfig
        ConnectionConfig config = buildConnectionConfig(request);

        // Establish connection to get detailed information
        connection = provider.connect(config);

        // Calculate ping time
        long ping = System.currentTimeMillis() - startTime;

        // Get database metadata
        DatabaseMetaData metaData;
        try {
            metaData = connection.getMetaData();
            // Build response
            String dbmsInfo = String.format("%s (ver. %s)",
                    metaData.getDatabaseProductName(),
                    metaData.getDatabaseProductVersion());

            String driverInfo = String.format("%s (ver. %s, JDBC%d.%d)",
                    metaData.getDriverName(),
                    metaData.getDriverVersion(),
                    metaData.getJDBCMajorVersion(),
                    metaData.getJDBCMinorVersion());

            // Close connection
            provider.closeConnection(connection);

            return ConnectionTestResponse.builder()
                    .status(ConnectionTestStatus.SUCCEEDED)
                    .dbmsInfo(dbmsInfo)
                    .driverInfo(driverInfo)
                    .ping(ping)
                    .build();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void closeConnection(String connectionId) {
        // Get connection
        Connection connection = activeConnections.get(connectionId);
        if (connection == null) {
            return;
        }

        // Get metadata for logging
        ConnectionMetadata metadata = connectionMetadata.get(connectionId);

        try {
            // Get plugin to close connection properly
            ConnectionProvider provider = getConnectionProviderByDbType(metadata.dbType());

            // Close connection
            provider.closeConnection(connection);

            log.info("Connection closed: connectionId={}, dbType={}, host={}",
                    connectionId, metadata.dbType(), metadata.host());

        } finally {
            // Always remove from registry
            activeConnections.remove(connectionId);
            connectionMetadata.remove(connectionId);
        }
    }

    /**
     * Get ConnectionProvider by database type.
     * Simply gets the first available plugin for the database type.
     * All plugins implementing ConnectionProvider have connection capability.
     *
     * @param dbTypeCode database type code string (e.g., "MYSQL", "mysql")
     * @return ConnectionProvider instance
     * @throws BusinessException if no plugin found for database type
     */
    private ConnectionProvider getConnectionProviderByDbType(String dbTypeCode) {
        // Get plugins by database type code (directly, no enum conversion needed)
        List<Plugin> plugins = pluginManager.getPluginsByDbTypeCode(dbTypeCode);
        if (plugins.isEmpty()) {
            throw new BusinessException(404,
                    "No plugin available for database type: " + dbTypeCode);
        }

        // Get first available plugin (all plugins have connection capability)
        Plugin plugin = plugins.get(0);

        // Cast to ConnectionProvider (all plugins with this dbType implement it)
        return (ConnectionProvider) plugin;
    }

    /**
     * Build ConnectionConfig from request DTO.
     *
     * @param request connect request
     * @return ConnectionConfig instance
     */
    private ConnectionConfig buildConnectionConfig(ConnectRequest request) {
        ConnectionConfig config = new ConnectionConfig();
        config.setHost(request.getHost());
        config.setPort(request.getPort());
        config.setDatabase(request.getDatabase());
        config.setUsername(request.getUsername());
        config.setPassword(request.getPassword());
        config.setDriverJarPath(request.getDriverJarPath());
        config.setTimeout(request.getTimeout());
        config.setProperties(request.getProperties());
        return config;
    }


    /**
     * Internal class to store connection metadata.
     */
    private record ConnectionMetadata(String dbType, String host) {}
}

