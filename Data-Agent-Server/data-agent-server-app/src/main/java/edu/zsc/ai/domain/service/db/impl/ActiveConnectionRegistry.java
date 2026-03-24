package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.common.constant.ResponseCode;
import edu.zsc.ai.common.constant.ResponseMessageKey;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Slf4j
public class ActiveConnectionRegistry {

    private static final int CONNECTION_VALIDATION_TIMEOUT_SECONDS = 1;

    /**
     * Active connection record.
     * Stores the logical connection group and its metadata.
     */
    public record ActiveConnection(
            DataSource dataSource,
            Long userId,
            Long dbConnectionId,
            String dbType,
            String pluginId,
            String databaseName,
            String schemaName,
            LocalDateTime createdAt,
            LocalDateTime lastAccessedAt) {

        public ActiveConnection touch() {
            return new ActiveConnection(
                    dataSource,
                    userId,
                    dbConnectionId,
                    dbType,
                    pluginId,
                    databaseName,
                    schemaName,
                    createdAt,
                    LocalDateTime.now()
            );
        }

        public BorrowedConnection borrowConnection() {
            return ActiveConnectionRegistry.borrowConnection(this);
        }
    }

    public record BorrowedConnection(
            Connection connection,
            ActiveConnection activeConnection) implements AutoCloseable {

        public String pluginId() {
            return activeConnection.pluginId();
        }

        @Override
        public void close() {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to close database connection: " + e.getMessage(), e);
            }
        }
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
     * Package-scoped registration hook used by tests and internal bootstrapping only.
     */
    static void registerConnection(Long dbConnectionId, ActiveConnection activeConnection) {
        String innerKey = generateInnerKey(activeConnection.databaseName(), activeConnection.schemaName());
        ActiveConnection previous = activeConnections.computeIfAbsent(dbConnectionId, k -> new ConcurrentHashMap<>())
                .put(innerKey, activeConnection);

        if (previous != null && previous.dataSource() != activeConnection.dataSource()) {
            doClose(previous);
        }

        log.info("Connection registered: dbConnectionId={}, key={}, dbType={}",
                dbConnectionId, innerKey, activeConnection.dbType());
    }

    public static ActiveConnection getOrCreateConnection(DbContext db, Supplier<ActiveConnection> activeConnectionSupplier) {
        Map<String, ActiveConnection> innerMap = activeConnections.computeIfAbsent(db.connectionId(), k -> new ConcurrentHashMap<>());
        String innerKey = generateInnerKey(db.catalog(), db.schema());

        return innerMap.compute(innerKey, (key, existing) -> {
            if (isConnectionUsable(existing)) {
                return existing.touch();
            }

            ActiveConnection created = activeConnectionSupplier.get();
            if (existing != null && existing.dataSource() != created.dataSource()) {
                doClose(existing);
            }

            log.info("Connection registered: dbConnectionId={}, key={}, dbType={}",
                    db.connectionId(), innerKey, created.dbType());
            return created;
        });
    }

    /**
     * Get active connection for a DbContext.
     */
    public static Optional<ActiveConnection> getConnection(DbContext db) {
        String key = generateInnerKey(db.catalog(), db.schema());
        return getUsableConnection(db.connectionId(), key);
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
        Optional<ActiveConnection> rootConnection = getUsableConnection(dbConnectionId, rootKey);
        if (rootConnection.isPresent()) {
            return rootConnection;
        }

        for (String key : innerMap.keySet()) {
            if (rootKey.equals(key)) {
                continue;
            }

            Optional<ActiveConnection> active = getUsableConnection(dbConnectionId, key);
            if (active.isPresent()) {
                return active;
            }
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
            innerMap.values().forEach(ActiveConnectionRegistry::doClose);
        }
    }

    public static BorrowedConnection borrowOwnedConnection(DbContext db) {
        return borrowConnection(getOwnedConnection(db));
    }

    public static BorrowedConnection borrowAnyOwnedConnection(Long dbConnectionId) {
        return borrowConnection(getAnyOwnedActiveConnection(dbConnectionId));
    }

    private static BorrowedConnection borrowConnection(ActiveConnection active) {
        try {
            Connection connection = active.dataSource().getConnection();
            return new BorrowedConnection(connection, active);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to borrow database connection: " + e.getMessage(), e);
        }
    }

    private static void doClose(ActiveConnection active) {
        try {
            if (active.dataSource() instanceof AutoCloseable autoCloseable) {
                autoCloseable.close();
            }
            log.info("Connection closed: dbConnectionId={}, database={}, schema={}",
                    active.dbConnectionId(), active.databaseName(), active.schemaName());
        } catch (Exception e) {
            log.error("Error closing connection: dbConnectionId={}", active.dbConnectionId(), e);
        }
    }

    private static boolean isConnectionUsable(ActiveConnection active) {
        if (active == null || active.dataSource() == null) {
            return false;
        }

        try (BorrowedConnection borrowedConnection = borrowConnection(active)) {
            Connection connection = borrowedConnection.connection();
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
        } catch (RuntimeException e) {
            log.warn("Failed to borrow connection, treating it as stale: dbConnectionId={}", active.dbConnectionId(), e);
            return false;
        }
    }

    private static Optional<ActiveConnection> getUsableConnection(Long dbConnectionId, String innerKey) {
        Map<String, ActiveConnection> innerMap = activeConnections.get(dbConnectionId);
        if (innerMap == null) {
            return Optional.empty();
        }

        AtomicReference<ActiveConnection> removedConnection = new AtomicReference<>();
        ActiveConnection active = innerMap.computeIfPresent(innerKey, (key, existing) -> {
            if (isConnectionUsable(existing)) {
                return existing.touch();
            }
            removedConnection.set(existing);
            return null;
        });

        cleanupEmptyGroup(dbConnectionId, innerMap);
        ActiveConnection removed = removedConnection.get();
        if (removed != null) {
            log.warn("Discarding stale connection: dbConnectionId={}, key={}, dbType={}",
                    dbConnectionId, innerKey, removed.dbType());
            doClose(removed);
        }
        return Optional.ofNullable(active);
    }

    private static void cleanupEmptyGroup(Long dbConnectionId, Map<String, ActiveConnection> innerMap) {
        if (innerMap.isEmpty()) {
            activeConnections.remove(dbConnectionId, innerMap);
        }
    }
}
