package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.agent.tool.sql.model.NamedObjectDetail;
import edu.zsc.ai.agent.tool.sql.model.ObjectQueryItem;
import edu.zsc.ai.agent.tool.sql.model.ObjectSearchResponse;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.AgentRequestContextInfo;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionResponse;
import edu.zsc.ai.domain.service.db.ConnectionAccessService;
import edu.zsc.ai.domain.service.db.DatabaseObjectService;
import edu.zsc.ai.domain.service.db.DatabaseService;
import edu.zsc.ai.domain.service.db.DbConnectionService;
import edu.zsc.ai.domain.service.db.IndexService;
import edu.zsc.ai.domain.service.db.SchemaService;
import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DiscoveryServiceImplTest {

    private final Executor executor = Runnable::run;
    private final DbConnectionService dbConnectionService = mock(DbConnectionService.class);
    private final DatabaseService databaseService = mock(DatabaseService.class);
    private final SchemaService schemaService = mock(SchemaService.class);
    private final DatabaseObjectService databaseObjectService = mock(DatabaseObjectService.class);
    private final IndexService indexService = mock(IndexService.class);
    private final ConnectionAccessService connectionAccessService = mock(ConnectionAccessService.class);

    private DiscoveryServiceImpl discoveryService;

    @BeforeEach
    void setUp() {
        doNothing().when(connectionAccessService).assertReadable(anyLong());
        discoveryService = new DiscoveryServiceImpl(
                executor,
                dbConnectionService,
                databaseService,
                schemaService,
                databaseObjectService,
                indexService,
                connectionAccessService
        );
    }

    @AfterEach
    void tearDown() {
        AgentRequestContext.clear();
        RequestContext.clear();
    }

    @Test
    void searchObjects_inExplorerScope_usesAllowedConnectionsOnly() {
        AgentRequestContext.set(AgentRequestContextInfo.builder()
                .agentType(AgentTypeEnum.EXPLORER.getCode())
                .allowedConnectionIds(List.of(5L, 7L))
                .build());

        when(dbConnectionService.getConnectionById(5L)).thenReturn(connection(5L));
        when(dbConnectionService.getConnectionById(7L)).thenReturn(connection(7L));
        when(databaseService.getDatabases(5L)).thenReturn(List.of("db5"));
        when(databaseService.getDatabases(7L)).thenReturn(List.of("db7"));
        when(schemaService.listSchemas(anyLong(), anyString())).thenReturn(List.of("public"));
        when(databaseObjectService.searchObjects(eq(DatabaseObjectTypeEnum.TABLE), eq("%user%"), any(DbContext.class), isNull()))
                .thenAnswer(invocation -> {
                    DbContext db = invocation.getArgument(2);
                    return List.of("users_" + db.connectionId());
                });

        ObjectSearchResponse response = discoveryService.searchObjects(
                "%user%", DatabaseObjectTypeEnum.TABLE, null, null, null);

        assertEquals(2, response.results().size());
        assertEquals(List.of(5L, 7L), response.results().stream().map(item -> item.connectionId()).toList());
        verify(dbConnectionService, never()).getAllConnections();
    }

    @Test
    void searchObjects_filtersDatabasesAndSchemasBySqlWildcardPattern() {
        when(dbConnectionService.getConnectionById(5L)).thenReturn(connection(5L));
        when(databaseService.getDatabases(5L)).thenReturn(List.of("app_core", "analytics"));
        when(schemaService.listSchemas(5L, "app_core")).thenReturn(List.of("public", "internal"));
        when(schemaService.listSchemas(5L, "analytics")).thenReturn(List.of("public"));
        when(databaseObjectService.searchObjects(
                eq(DatabaseObjectTypeEnum.TABLE),
                eq("%user%"),
                eq(new DbContext(5L, "app_core", "public")),
                isNull()
        )).thenReturn(List.of("users"));

        ObjectSearchResponse response = discoveryService.searchObjects(
                "%user%", DatabaseObjectTypeEnum.TABLE, 5L, "app_%", "pub%");

        assertEquals(1, response.results().size());
        assertEquals("app_core", response.results().get(0).databaseName());
        assertEquals("public", response.results().get(0).schemaName());
        verify(databaseObjectService, never()).searchObjects(
                eq(DatabaseObjectTypeEnum.TABLE),
                eq("%user%"),
                argThat(db -> db != null && "analytics".equals(db.catalog())),
                isNull()
        );
        verify(databaseObjectService, never()).searchObjects(
                eq(DatabaseObjectTypeEnum.TABLE),
                eq("%user%"),
                eq(new DbContext(5L, "app_core", "internal")),
                isNull()
        );
    }

    @Test
    void getObjectDetails_inExplorerScope_marksOutOfScopeItemsAsFailed() {
        RequestContext.set(RequestContextInfo.builder()
                .connectionId(5L)
                .build());
        AgentRequestContext.set(AgentRequestContextInfo.builder()
                .agentType(AgentTypeEnum.EXPLORER.getCode())
                .allowedConnectionIds(List.of(5L))
                .build());

        List<NamedObjectDetail> results = discoveryService.getObjectDetails(List.of(
                new ObjectQueryItem("TABLE", "users", 7L, "app", "public")
        ));

        assertEquals(1, results.size());
        assertFalse(results.get(0).success());
        assertTrue(results.get(0).error().contains("not allowed"));
    }

    @Test
    void getObjectDetails_returnsFlattenedDetailWithEffectiveScope() {
        when(databaseObjectService.getObjectDdl(DatabaseObjectTypeEnum.TABLE, "users", new DbContext(5L, "app", "public")))
                .thenReturn("CREATE TABLE users ...");
        when(databaseObjectService.countObjectRows(DatabaseObjectTypeEnum.TABLE, new DbContext(5L, "app", "public"), "users"))
                .thenReturn(12L);
        when(indexService.getIndexes(new DbContext(5L, "app", "public"), "users"))
                .thenReturn(List.of());

        List<NamedObjectDetail> results = discoveryService.getObjectDetails(List.of(
                new ObjectQueryItem("TABLE", "users", 5L, "app", "public")
        ));

        assertEquals(1, results.size());
        NamedObjectDetail detail = results.get(0);
        assertTrue(detail.success());
        assertEquals(5L, detail.connectionId());
        assertEquals("app", detail.databaseName());
        assertEquals("public", detail.schemaName());
        assertEquals("CREATE TABLE users ...", detail.ddl());
        assertEquals(12L, detail.rowCount());
        assertEquals(List.of(), detail.indexes());
    }

    private static ConnectionResponse connection(Long id) {
        return ConnectionResponse.builder()
                .id(id)
                .name("conn-" + id)
                .dbType("postgresql")
                .build();
    }
}
