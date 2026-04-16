package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.common.enums.org.WorkspaceTypeEnum;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.service.db.ConnectionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchemaServiceImplTest {

    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final SchemaServiceImpl schemaService = new SchemaServiceImpl(connectionService);
    private Connection registeredConnection;

    @AfterEach
    void tearDown() throws Exception {
        RequestContext.clear();
        activeConnections().clear();
    }

    @Test
    void listSchemas_returnsEmptyListWhenPluginDoesNotSupportSchema() throws Exception {
        RequestContext.set(RequestContextInfo.builder().userId(7L).build());
        register(21L, 7L, "mysql-5.7");

        List<String> schemas = schemaService.listSchemas(21L, "app");

        assertEquals(List.of(), schemas);
        verify(connectionService).openConnection(21L);
        verify(registeredConnection, never()).getMetaData();
    }

    private Connection usableConnection() throws SQLException {
        Connection connection = mock(Connection.class);
        when(connection.isClosed()).thenReturn(false);
        when(connection.isValid(1)).thenReturn(true);
        return connection;
    }

    private void register(Long connectionId, Long userId, String pluginId) throws Exception {
        Connection connection = usableConnection();
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);
        ActiveConnectionRegistry.registerConnection(
                connectionId,
                new ActiveConnectionRegistry.ActiveConnection(
                        dataSource,
                        userId,
                        userId,
                        connectionId,
                        "mysql",
                        pluginId,
                        "app",
                        null,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        WorkspaceTypeEnum.PERSONAL,
                        null
                )
        );
        registeredConnection = connection;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Map<String, ActiveConnectionRegistry.ActiveConnection>> activeConnections() throws Exception {
        Field field = ActiveConnectionRegistry.class.getDeclaredField("activeConnections");
        field.setAccessible(true);
        return (Map<Long, Map<String, ActiveConnectionRegistry.ActiveConnection>>) field.get(null);
    }
}
