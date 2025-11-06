package edu.zsc.ai.service.impl;

import edu.zsc.ai.converter.ConnectionConfigConverter;
import edu.zsc.ai.model.dto.request.ConnectRequest;
import edu.zsc.ai.model.dto.response.ConnectionTestResponse;
import edu.zsc.ai.model.dto.response.OpenConnectionResponse;
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

    @Override
    public ConnectionTestResponse testConnection(ConnectRequest request) {
        Connection connection = null;
        long startTime = System.currentTimeMillis();
        // Get ConnectionProvider for the database type
        ConnectionProvider provider = PluginManager.getConnectionProvider(request.getDbType());
        // Convert request to ConnectionConfig
        ConnectionConfig config = ConnectionConfigConverter.convert(request);

        try {
        // Establish connection to get detailed information
        connection = provider.connect(config);

        // Calculate ping time
        long ping = System.currentTimeMillis() - startTime;

            // Get database and driver information using ConnectionProvider
            String dbmsInfo = provider.getDbmsInfo(connection);
            String driverInfo = provider.getDriverInfo(connection);

            return ConnectionTestResponse.builder()
                    .status(ConnectionTestStatus.SUCCEEDED)
                    .dbmsInfo(dbmsInfo)
                    .driverInfo(driverInfo)
                    .ping(ping)
                    .build();
        } finally {
            // Ensure connection is closed
            if (connection != null) {
                try {
                    provider.closeConnection(connection);
                } catch (Exception e) {
                    log.warn("Failed to close connection", e);
                }
            }
        }
    }

    @Override
    public OpenConnectionResponse openConnection(ConnectRequest request) {
        // Get ConnectionProvider for initial connection (to get database version)
        ConnectionProvider currentProvider = PluginManager.getConnectionProvider(request.getDbType());
        Plugin initialPlugin = (Plugin) currentProvider;

        // Convert request to ConnectionConfig
        ConnectionConfig config = ConnectionConfigConverter.convert(request);

        // Establish connection first
        Connection connection = currentProvider.connect(config);

        try {
            // Get database version from connection using ConnectionProvider
            String databaseVersion = currentProvider.getDatabaseProductVersion(connection);
            
            // Select appropriate plugin based on database version
            Plugin selectedPlugin = PluginManager.selectPluginByDbVersion(request.getDbType(), databaseVersion);
            
            // Close the initial connection and reconnect with the selected plugin if needed
            if (!selectedPlugin.getPluginId().equals(initialPlugin.getPluginId())) {
                // If plugin changed, close current connection and reconnect with new plugin
                currentProvider.closeConnection(connection);
                ConnectionProvider selectedProvider = (ConnectionProvider) selectedPlugin;
                connection = selectedProvider.connect(config);
                currentProvider = selectedProvider; // Update current provider for error handling
            }
            
            // Store connection in ConnectionManager with selected plugin's ID
            String connectionId = ConnectionManager.openConnection(request, connection, selectedPlugin.getPluginId());

        // Get metadata for response
            ConnectionManager.ConnectionMetadata metadata = ConnectionManager.getConnectionMetadata(connectionId);

        // Build response
        return OpenConnectionResponse.builder()
                .connectionId(connectionId)
                .dbType(request.getDbType())
                .host(request.getHost())
                .port(request.getPort())
                .database(request.getDatabase())
                .username(request.getUsername())
                .connected(true)
                .createdAt(metadata.createdAt())
                .build();
        } catch (Exception e) {
            // Close connection on error before rethrowing
            // Use currentProvider which might have been updated if plugin changed
            if (connection != null) {
                try {
                    currentProvider.closeConnection(connection);
                } catch (Exception closeException) {
                    log.warn("Failed to close connection after error", closeException);
                }
            }
            throw e;
        }
    }

    @Override
    public void closeConnection(String connectionId) {
        ConnectionManager.closeConnection(connectionId);
    }
}

