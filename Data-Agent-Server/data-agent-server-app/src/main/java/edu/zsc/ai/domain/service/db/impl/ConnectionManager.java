package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.common.converter.db.ConnectionMetadataConverter;
import edu.zsc.ai.util.exception.BusinessException;
import edu.zsc.ai.domain.model.dto.request.db.ConnectRequest;
import edu.zsc.ai.plugin.capability.ConnectionProvider;
import edu.zsc.ai.plugin.manager.PluginManager;
import edu.zsc.ai.util.HashUtil;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Connection Manager
 * Thread-safe static utility class for managing active database connections.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Slf4j
public class ConnectionManager {

    /**
     * Connection metadata record.
     * Stores information about a connection for lifecycle management.
     */
    public record ConnectionMetadata(
            String dbType,
            String host,
            Integer port,
            String database,
            String username,
            String pluginId,
            LocalDateTime createdAt,
            LocalDateTime lastAccessedAt) {

        /**
         * Generate connectionId based on this metadata.
         * Uses hash of key fields (excluding createdAt and lastAccessedAt) to ensure same configuration produces same ID.
         *
         * @return connection identifier (SHA-256 hash string)
         */
        public String generateConnectionId() {
            // Build hash string from key fields (excluding createdAt and lastAccessedAt for consistency)
            String hashInput = String.join("|",
                    dbType != null ? dbType : "",
                    host != null ? host : "",
                    port != null ? String.valueOf(port) : "",
                    database != null ? database : "",
                    username != null ? username : "",
                    pluginId != null ? pluginId : ""
            );
            return HashUtil.sha256(hashInput);
        }
    }

    /**
     * Active connections registry: connectionId -> Connection
     */
    private static final Map<String, Connection> activeConnections = new ConcurrentHashMap<>();

    /**
     * Connection metadata registry: connectionId -> ConnectionMetadata
     */
    private static final Map<String, ConnectionMetadata> connectionMetadata = new ConcurrentHashMap<>();

    /**
     * Open a new database connection and store it in the active connections registry.
     * Generates a connectionId based on connection metadata hash to enable connection reuse.
     * If a connection with the same configuration already exists, it will be reused.
     *
     * @param request    connection request with connection parameters
     * @param connection the established database connection
     * @param pluginId   the plugin ID used to establish this connection
     * @return unique connection identifier (hash-based)
     */
    public static String openConnection(ConnectRequest request, Connection connection, String pluginId) {
        // Create metadata using converter
        ConnectionMetadata metadata = ConnectionMetadataConverter.convert(request, pluginId);

        // Generate connectionId based on metadata
        String connectionId = metadata.generateConnectionId();

        // Check if connection already exists
        Connection existingConnection = activeConnections.get(connectionId);
        if (existingConnection == null) {
            // Store new connection and metadata
            activeConnections.put(connectionId, connection);
            connectionMetadata.put(connectionId, metadata);
            log.info("Connection opened: connectionId={}, dbType={}, host={}, database={}",
                    connectionId, request.getDbType(), request.getHost(), request.getDatabase());
            return connectionId;
        }
        return connectionId;
    }

    /**
     * Close an active database connection and remove it from the registry.
     *
     * @param connectionId unique connection identifier
     * @throws BusinessException if connection not found
     */
    public static void closeConnection(String connectionId) {
        try {
            Connection connection = activeConnections.get(connectionId);
            ConnectionMetadata metadata = connectionMetadata.get(connectionId);

            // Get plugin to close connection properly
            ConnectionProvider provider = PluginManager.selectConnectionProviderByPluginId(metadata.pluginId());

            // Close connection
            provider.closeConnection(connection);

            log.info("Connection closed: connectionId={}, dbType={}, host={}",
                    connectionId, metadata.dbType(), metadata.host());

        } catch (Exception e) {
            log.error("Error closing connection: connectionId={}", connectionId, e);
            // Continue to remove from registry even if close fails
        } finally {
            // Always remove from registry
            activeConnections.remove(connectionId);
            connectionMetadata.remove(connectionId);
        }
    }


    /**
     * Get connection metadata by connectionId.
     * Updates lastAccessedAt timestamp when metadata is accessed.
     *
     * @param connectionId unique connection identifier
     * @return ConnectionMetadata if exists, null otherwise
     */
    public static ConnectionMetadata getConnectionMetadata(String connectionId) {
        ConnectionMetadata metadata = connectionMetadata.get(connectionId);
        if (metadata != null) {
            updateLastAccessedAt(connectionId);
        }
        return metadata;
    }

    /**
     * Update lastAccessedAt timestamp for a connection.
     *
     * @param connectionId unique connection identifier
     */
    private static void updateLastAccessedAt(String connectionId) {
        ConnectionMetadata existingMetadata = connectionMetadata.get(connectionId);
        if (existingMetadata != null) {
            ConnectionMetadata updatedMetadata = new ConnectionMetadata(
                    existingMetadata.dbType(),
                    existingMetadata.host(),
                    existingMetadata.port(),
                    existingMetadata.database(),
                    existingMetadata.username(),
                    existingMetadata.pluginId(),
                    existingMetadata.createdAt(),
                    LocalDateTime.now()
            );
            connectionMetadata.put(connectionId, updatedMetadata);
        }
    }


}

