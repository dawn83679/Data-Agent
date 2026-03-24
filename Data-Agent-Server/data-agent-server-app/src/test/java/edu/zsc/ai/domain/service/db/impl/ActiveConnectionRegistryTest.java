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
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActiveConnectionRegistryTest {

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
        DataSource dataSource = dataSource(staleConnection);

        register(11L, 7L, "sales", null, dataSource);

        assertTrue(ActiveConnectionRegistry.getConnection(new DbContext(11L, "sales", null)).isEmpty());
        assertTrue(ActiveConnectionRegistry.getAnyActiveConnection(11L).isEmpty());
    }

    @Test
    void getAnyOwnedActiveConnection_prefersRootConnectionWhenItIsUsable() throws SQLException {
        RequestContext.set(RequestContextInfo.builder().userId(7L).build());

        Connection schemaConnection = usableConnection();
        Connection rootConnection = usableConnection();
        DataSource schemaDataSource = dataSource(schemaConnection);
        DataSource rootDataSource = dataSource(rootConnection);

        register(21L, 7L, "sales", null, schemaDataSource);
        register(21L, 7L, null, null, rootDataSource);

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getAnyOwnedActiveConnection(21L);

        assertSame(rootDataSource, active.dataSource());
    }

    @Test
    void getAnyOwnedActiveConnection_skipsStaleRootConnection() throws SQLException {
        RequestContext.set(RequestContextInfo.builder().userId(7L).build());

        Connection staleRootConnection = mock(Connection.class);
        when(staleRootConnection.isClosed()).thenReturn(true);
        DataSource staleRootDataSource = dataSource(staleRootConnection);

        Connection schemaConnection = usableConnection();
        DataSource schemaDataSource = dataSource(schemaConnection);

        register(31L, 7L, null, null, staleRootDataSource);
        register(31L, 7L, "sales", null, schemaDataSource);

        ActiveConnectionRegistry.ActiveConnection active = ActiveConnectionRegistry.getAnyOwnedActiveConnection(31L);

        assertSame(schemaDataSource, active.dataSource());
    }

    private Connection usableConnection() throws SQLException {
        Connection connection = mock(Connection.class);
        when(connection.isClosed()).thenReturn(false);
        when(connection.isValid(1)).thenReturn(true);
        return connection;
    }

    private DataSource dataSource(Connection connection) throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);
        return dataSource;
    }

    private void register(Long connectionId,
                          Long userId,
                          String catalog,
                          String schema,
                          DataSource dataSource) {
        ActiveConnectionRegistry.registerConnection(
                connectionId,
                new ActiveConnectionRegistry.ActiveConnection(
                        dataSource,
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
    private Map<Long, Map<String, ActiveConnectionRegistry.ActiveConnection>> activeConnections() throws Exception {
        Field field = ActiveConnectionRegistry.class.getDeclaredField("activeConnections");
        field.setAccessible(true);
        return (Map<Long, Map<String, ActiveConnectionRegistry.ActiveConnection>>) field.get(null);
    }
}
