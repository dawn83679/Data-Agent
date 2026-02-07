package edu.zsc.ai.domain.service.db.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.common.constant.ResponseCode;
import edu.zsc.ai.common.constant.ResponseMessageKey;
import edu.zsc.ai.plugin.capability.ConnectionProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.util.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ConnectionManager {

    /**
     * Active connection record.
     * Stores the physical connection and its metadata.
     */
    public record ActiveConnection(
            Connection connection,
            Long userId,
            Long dbConnectionId,
            String dbType,
            String pluginId,
            String databaseName,
            String schemaName,
            LocalDateTime createdAt,
            LocalDateTime lastAccessedAt) {
    }

    /**
     * Active connections registry: dbConnectionId -> { database_schema -> ActiveConnection }
     */
    private static final Map<Long, Map<String, ActiveConnection>> activeConnections = new ConcurrentHashMap<>();

    /**
     * Generate inner key for the second level map.
     */
    public static String generateInnerKey(String databaseName, String schemaName) {
        return (databaseName != null ? databaseName : "null") + "::" + (schemaName != null ? schemaName : "null");
    }

    /**
     * Register a new active connection.
     */
    public static void registerConnection(Long dbConnectionId, ActiveConnection activeConnection) {
        String innerKey = generateInnerKey(activeConnection.databaseName(), activeConnection.schemaName());
        activeConnections.computeIfAbsent(dbConnectionId, k -> new ConcurrentHashMap<>())
                .put(innerKey, activeConnection);
        
        log.info("Connection registered: dbConnectionId={}, key={}, dbType={}",
                dbConnectionId, innerKey, activeConnection.dbType());
    }

    /**
     * Get active connection for a dbConnectionId and specific catalog/schema.
     */
    public static Optional<ActiveConnection> getConnection(Long dbConnectionId, String catalog, String schema) {
        Map<String, ActiveConnection> innerMap = activeConnections.get(dbConnectionId);
        if (innerMap == null) {
            return Optional.empty();
        }
        String key = generateInnerKey(catalog, schema);
        return Optional.ofNullable(innerMap.get(key));
    }

    /**
     * Get active connection for a dbConnectionId and specific catalog/schema, 
     * and verify ownership by current user.
     */
    public static ActiveConnection getOwnedConnection(Long dbConnectionId, String catalog, String schema) {
        ActiveConnection active = getConnection(dbConnectionId, catalog, schema)
                .orElseThrow(() -> BusinessException.notFound(ResponseMessageKey.CONNECTION_ACCESS_DENIED_MESSAGE));
        
        if (!active.userId().equals(StpUtil.getLoginIdAsLong())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, ResponseMessageKey.CONNECTION_ACCESS_DENIED_MESSAGE);
        }
        return active;
    }

    /**
     * Get any active connection for a dbConnectionId (e.g. for listing databases).
     */
    public static Optional<ActiveConnection> getAnyActiveConnection(Long dbConnectionId) {
        return Optional.ofNullable(activeConnections.get(dbConnectionId))
                .flatMap(m -> m.values().stream().findFirst());
    }

    /**
     * Get any active connection for a dbConnectionId and verify ownership.
     */
    public static ActiveConnection getAnyOwnedActiveConnection(Long dbConnectionId) {
        ActiveConnection active = getAnyActiveConnection(dbConnectionId)
                .orElseThrow(() -> BusinessException.notFound(ResponseMessageKey.CONNECTION_ACCESS_DENIED_MESSAGE));

        if (!active.userId().equals(StpUtil.getLoginIdAsLong())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, ResponseMessageKey.CONNECTION_ACCESS_DENIED_MESSAGE);
        }
        return active;
    }

    /**
     * Close all connections for a dbConnectionId.
     */
    public static void closeAllConnections(Long dbConnectionId) {
        Map<String, ActiveConnection> innerMap = activeConnections.remove(dbConnectionId);
        if (innerMap != null) {
            innerMap.values().forEach(ConnectionManager::doClose);
        }
    }

    private static void doClose(ActiveConnection active) {
        try {
            ConnectionProvider provider = DefaultPluginManager.getInstance()
                    .getConnectionProviderByPluginId(active.pluginId());
            provider.closeConnection(active.connection());
            log.info("Connection closed: dbConnectionId={}, database={}, schema={}",
                    active.dbConnectionId(), active.databaseName(), active.schemaName());
        } catch (Exception e) {
            log.error("Error closing connection: dbConnectionId={}", active.dbConnectionId(), e);
        }
    }
}
