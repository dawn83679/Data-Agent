package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.common.constant.ResponseCode;
import edu.zsc.ai.common.constant.ResponseMessageKey;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.plugin.capability.ConnectionProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.domain.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ConnectionManager {

    private static final int CONNECTION_VALIDATION_TIMEOUT_SECONDS = 1;

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
        ActiveConnection previous = activeConnections.computeIfAbsent(dbConnectionId, k -> new ConcurrentHashMap<>())
                .put(innerKey, activeConnection);

        if (previous != null && previous.connection() != activeConnection.connection()) {
            doClose(previous);
        }

        log.info("Connection registered: dbConnectionId={}, key={}, dbType={}",
                dbConnectionId, innerKey, activeConnection.dbType());
    }

    /**
     * Get active connection for a DbContext.
     */
    public static Optional<ActiveConnection> getConnection(DbContext db) {
        Map<String, ActiveConnection> innerMap = activeConnections.get(db.connectionId());
        if (innerMap == null) {
            return Optional.empty();
        }
        String key = generateInnerKey(db.catalog(), db.schema());
        ActiveConnection active = innerMap.get(key);
        if (!isConnectionUsable(active)) {
            removeInvalidConnection(db.connectionId(), key, active);
            return Optional.empty();
        }
        return Optional.of(active);
    }

    /**
     * Get active connection for a DbContext,
     * and verify ownership by the current user from RequestContext.
     */
    public static ActiveConnection getOwnedConnection(DbContext db) {
        Long userId = RequestContext.getUserId();
        if (userId == null) {
            throw new IllegalStateException("No userId available in RequestContext");
        }

        ActiveConnection active = getConnection(db)
                .orElseThrow(() -> BusinessException.notFound(ResponseMessageKey.CONNECTION_ACCESS_DENIED_MESSAGE));

        if (!active.userId().equals(userId)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, ResponseMessageKey.CONNECTION_ACCESS_DENIED_MESSAGE);
        }
        return active;
    }

    /**
     * Get any active connection for a dbConnectionId (e.g. for listing databases).
     */
    public static Optional<ActiveConnection> getAnyActiveConnection(Long dbConnectionId) {
        Map<String, ActiveConnection> innerMap = activeConnections.get(dbConnectionId);
        if (innerMap == null) {
            return Optional.empty();
        }

        String rootKey = generateInnerKey(null, null);
        ActiveConnection rootConnection = innerMap.get(rootKey);
        if (isConnectionUsable(rootConnection)) {
            return Optional.of(rootConnection);
        }
        removeInvalidConnection(dbConnectionId, rootKey, rootConnection);

        for (Map.Entry<String, ActiveConnection> entry : innerMap.entrySet()) {
            if (rootKey.equals(entry.getKey())) {
                continue;
            }

            ActiveConnection active = entry.getValue();
            if (isConnectionUsable(active)) {
                return Optional.of(active);
            }
            removeInvalidConnection(dbConnectionId, entry.getKey(), active);
        }

        cleanupEmptyGroup(dbConnectionId, innerMap);
        return Optional.empty();
    }

    /**
     * Get any active connection for a dbConnectionId and verify ownership
     * by the current user from RequestContext.
     */
    public static ActiveConnection getAnyOwnedActiveConnection(Long dbConnectionId) {
        Long userId = RequestContext.getUserId();
        if (userId == null) {
            throw new IllegalStateException("No userId available in RequestContext");
        }

        ActiveConnection active = getAnyActiveConnection(dbConnectionId)
                .orElseThrow(() -> BusinessException.notFound(ResponseMessageKey.CONNECTION_ACCESS_DENIED_MESSAGE));

        if (!active.userId().equals(userId)) {
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

    private static boolean isConnectionUsable(ActiveConnection active) {
        if (active == null || active.connection() == null) {
            return false;
        }

        Connection connection = active.connection();
        try {
            if (connection.isClosed()) {
                return false;
            }
            return connection.isValid(CONNECTION_VALIDATION_TIMEOUT_SECONDS);
        } catch (SQLFeatureNotSupportedException e) {
            log.debug("Connection validation is not supported by driver, falling back to isClosed check: dbConnectionId={}",
                    active.dbConnectionId());
            return true;
        } catch (SQLException e) {
            log.warn("Failed to validate connection, treating it as stale: dbConnectionId={}", active.dbConnectionId(), e);
            return false;
        }
    }

    private static void removeInvalidConnection(Long dbConnectionId, String innerKey, ActiveConnection active) {
        if (active == null) {
            return;
        }

        Map<String, ActiveConnection> innerMap = activeConnections.get(dbConnectionId);
        if (innerMap == null) {
            return;
        }

        boolean removed = innerMap.remove(innerKey, active);
        cleanupEmptyGroup(dbConnectionId, innerMap);
        if (removed) {
            log.warn("Discarding stale connection: dbConnectionId={}, key={}, dbType={}",
                    dbConnectionId, innerKey, active.dbType());
            doClose(active);
        }
    }

    private static void cleanupEmptyGroup(Long dbConnectionId, Map<String, ActiveConnection> innerMap) {
        if (innerMap.isEmpty()) {
            activeConnections.remove(dbConnectionId, innerMap);
        }
    }
}
