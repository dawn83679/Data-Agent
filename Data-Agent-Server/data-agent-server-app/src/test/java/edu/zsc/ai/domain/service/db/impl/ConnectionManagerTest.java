package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.model.context.DbContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConnectionManagerTest {

    @AfterEach
    void tearDown() throws Exception {
        RequestContext.clear();
        activeConnections().clear();
    }

    @Test
    void getConnection_discardsConnectionWhenValidationFails() throws Exception {
        Connection staleConnection = mock(Connection.class);
        when(staleConnection.isClosed()).thenReturn(false);
        when(staleConnection.isValid(1)).thenReturn(false);

        register(11L, 7L, "sales", null, staleConnection);

        assertTrue(ConnectionManager.getConnection(new DbContext(11L, "sales", null)).isEmpty());
        assertTrue(ConnectionManager.getAnyActiveConnection(11L).isEmpty());
    }

    @Test
    void getAnyOwnedActiveConnection_prefersRootConnectionWhenItIsUsable() throws SQLException {
        RequestContext.set(RequestContextInfo.builder().userId(7L).build());

        Connection schemaConnection = usableConnection();
        Connection rootConnection = usableConnection();

        register(21L, 7L, "sales", null, schemaConnection);
        register(21L, 7L, null, null, rootConnection);

        ConnectionManager.ActiveConnection active = ConnectionManager.getAnyOwnedActiveConnection(21L);

        assertSame(rootConnection, active.connection());
    }

    @Test
    void getAnyOwnedActiveConnection_skipsStaleRootConnection() throws SQLException {
        RequestContext.set(RequestContextInfo.builder().userId(7L).build());

        Connection staleRootConnection = mock(Connection.class);
        when(staleRootConnection.isClosed()).thenReturn(true);

        Connection schemaConnection = usableConnection();

        register(31L, 7L, null, null, staleRootConnection);
        register(31L, 7L, "sales", null, schemaConnection);

        ConnectionManager.ActiveConnection active = ConnectionManager.getAnyOwnedActiveConnection(31L);

        assertSame(schemaConnection, active.connection());
    }

    private Connection usableConnection() throws SQLException {
        Connection connection = mock(Connection.class);
        when(connection.isClosed()).thenReturn(false);
        when(connection.isValid(1)).thenReturn(true);
        return connection;
    }

    private void register(Long connectionId,
                          Long userId,
                          String catalog,
                          String schema,
                          Connection connection) {
        ConnectionManager.registerConnection(
                connectionId,
                new ConnectionManager.ActiveConnection(
                        connection,
                        userId,
                        connectionId,
                        "mysql",
                        "mysql-8",
                        catalog,
                        schema,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                )
        );
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Map<String, ConnectionManager.ActiveConnection>> activeConnections() throws Exception {
        Field field = ConnectionManager.class.getDeclaredField("activeConnections");
        field.setAccessible(true);
        return (Map<Long, Map<String, ConnectionManager.ActiveConnection>>) field.get(null);
    }
}
