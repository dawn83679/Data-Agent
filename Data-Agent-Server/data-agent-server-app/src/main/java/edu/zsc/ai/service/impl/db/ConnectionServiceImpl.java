package edu.zsc.ai.service.impl.db;

import edu.zsc.ai.converter.db.ConnectionConfigConverter;
import edu.zsc.ai.model.dto.request.db.ConnectRequest;
import edu.zsc.ai.model.dto.response.db.ConnectionTestResponse;
import edu.zsc.ai.model.dto.response.db.OpenConnectionResponse;
import edu.zsc.ai.enums.db.ConnectionTestStatus;
import edu.zsc.ai.plugin.Plugin;
import edu.zsc.ai.plugin.capability.ConnectionProvider;
import edu.zsc.ai.plugin.manager.PluginManager;
import edu.zsc.ai.plugin.connection.ConnectionConfig;
import edu.zsc.ai.service.db.ConnectionService;
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
        ConnectionProvider provider = PluginManager.selectConnectionProviderByDbType(request.getDbType());

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
        // Select the first plugin for initial connection (to get database version)
        Plugin initialPlugin = PluginManager.selectFirstPluginByDbType(request.getDbType());
        ConnectionProvider conncetionProvider = (ConnectionProvider) initialPlugin;

        // Convert request to ConnectionConfig
        ConnectionConfig config = ConnectionConfigConverter.convert(request);

        // Establish connection first
        Connection connection = conncetionProvider.connect(config);

        // Get database version from connection using ConnectionProvider
        String databaseVersion = conncetionProvider.getDatabaseProductVersion(connection);

        // Select appropriate plugin based on database version
        Plugin selectedPlugin = PluginManager.selectPluginByDbTypeAndVersion(request.getDbType(), databaseVersion);

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

    }

    @Override
    public void closeConnection(String connectionId) {
        ConnectionManager.closeConnection(connectionId);
    }
}

