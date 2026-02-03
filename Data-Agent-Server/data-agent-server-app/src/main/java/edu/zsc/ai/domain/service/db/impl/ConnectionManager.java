package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.common.converter.db.ConnectionMetadataConverter;
import edu.zsc.ai.util.exception.BusinessException;
import edu.zsc.ai.domain.model.dto.request.db.ConnectRequest;
import edu.zsc.ai.plugin.capability.ConnectionProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.util.HashUtil;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
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
            Long userId,
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
         * Uses hash of key fields including userId so same config opened by different users gets different IDs.
         *
         * @return connection identifier (SHA-256 hash string)
         */
        public String generateConnectionId() {
            String hashInput = String.join("|",
                    userId != null ? String.valueOf(userId) : "",
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
     * Generates a connectionId based on connection metadata hash (including userId) to enable connection reuse per user.
     *
     * @param request    connection request with connection parameters
     * @param connection the established database connection
     * @param pluginId   the plugin ID used to establish this connection
     * @param userId     the current user ID who opens this connection
     * @return unique connection identifier (hash-based)
     */
    public static String openConnection(ConnectRequest request, Connection connection, String pluginId, Long userId) {
        ConnectionMetadata metadata = ConnectionMetadataConverter.convert(request, pluginId, userId);

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
        Connection connection = activeConnections.get(connectionId);
        ConnectionMetadata metadata = connectionMetadata.get(connectionId);

        if (connection == null || metadata == null) {
            log.warn("Close connection ignored: connectionId not found, connectionId={}", connectionId);
            return;
        }

        try {
            ConnectionProvider provider = DefaultPluginManager.getInstance().getConnectionProviderByPluginId(metadata.pluginId());
            provider.closeConnection(connection);
            log.info("Connection closed: connectionId={}, dbType={}, host={}",
                    connectionId, metadata.dbType(), metadata.host());
        } catch (Exception e) {
            log.error("Error closing connection: connectionId={}", connectionId, e);
        } finally {
            activeConnections.remove(connectionId);
            connectionMetadata.remove(connectionId);
        }
    }


    /**
     * Get connection by connectionId.
     * Does not modify metadata or update lastAccessedAt.
     *
     * @param connectionId unique connection identifier
     * @return Optional containing the Connection if exists, empty otherwise
     */
    public static Optional<Connection> getConnection(String connectionId) {
        Connection connection = activeConnections.get(connectionId);
        return Optional.ofNullable(connection);
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
                    existingMetadata.userId(),
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

