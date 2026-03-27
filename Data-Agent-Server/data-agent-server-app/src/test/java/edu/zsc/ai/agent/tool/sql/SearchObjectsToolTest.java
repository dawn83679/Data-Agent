package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.aspect.AgentToolContextAspect;
import edu.zsc.ai.agent.tool.error.ToolErrorMapper;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.sql.model.ObjectSearchQuery;
import edu.zsc.ai.agent.tool.sql.model.ObjectSearchResponse;
import edu.zsc.ai.agent.tool.sql.model.ObjectSearchResult;
import edu.zsc.ai.common.constant.InvocationContextConstant;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.service.db.DiscoveryService;
import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SearchObjectsToolTest {

    private final DiscoveryService discoveryService = mock(DiscoveryService.class);
    private final SearchObjectsTool tool = proxiedTool();

    @AfterEach
    void tearDown() {
        AgentRequestContext.clear();
        RequestContext.clear();
    }

    @Test
    void nonExplorerMode_fallsBackToRequestContextConnection() {
        when(discoveryService.searchObjects("%user%", null, 5L, null, null))
                .thenReturn(new ObjectSearchResponse(List.of(), 0, false, null));

        AgentToolResult result = tool.searchObjects(
                new ObjectSearchQuery("%user%", null, null, null, null),
                InvocationParameters.from(Map.of(InvocationContextConstant.CONNECTION_ID, "5"))
        );

        assertTrue(result.isSuccess());
        assertTrue(result.getElapsedMs() != null && result.getElapsedMs() >= 0);
        verify(discoveryService).searchObjects("%user%", null, 5L, null, null);
    }

    @Test
    void nonExplorerMode_inheritsDatabaseAndSchemaFromRequestContextWhenConnectionMatches() {
        when(discoveryService.searchObjects("chat2db_user", null, 5L, "app", "public"))
                .thenReturn(new ObjectSearchResponse(List.of(), 0, false, null));

        AgentToolResult result = tool.searchObjects(
                new ObjectSearchQuery("chat2db_user", null, 5L, null, null),
                InvocationParameters.from(Map.of(
                        InvocationContextConstant.CONNECTION_ID, "5",
                        InvocationContextConstant.DATABASE_NAME, "app",
                        InvocationContextConstant.SCHEMA_NAME, "public"
                ))
        );

        assertTrue(result.isSuccess());
        verify(discoveryService).searchObjects("chat2db_user", null, 5L, "app", "public");
    }

    @Test
    void nonExplorerMode_doesNotInheritDatabaseAndSchemaAcrossDifferentConnections() {
        when(discoveryService.searchObjects("chat2db_user", null, 7L, null, null))
                .thenReturn(new ObjectSearchResponse(List.of(), 0, false, null));

        AgentToolResult result = tool.searchObjects(
                new ObjectSearchQuery("chat2db_user", null, 7L, null, null),
                InvocationParameters.from(Map.of(
                        InvocationContextConstant.CONNECTION_ID, "5",
                        InvocationContextConstant.DATABASE_NAME, "app",
                        InvocationContextConstant.SCHEMA_NAME, "public"
                ))
        );

        assertTrue(result.isSuccess());
        verify(discoveryService).searchObjects("chat2db_user", null, 7L, null, null);
    }

    @Test
    void explorerMode_withoutDatabaseScope_searchesAllowedConnectionsInsteadOfDefaultOne() {
        when(discoveryService.searchObjects("%user%", null, null, null, null))
                .thenReturn(new ObjectSearchResponse(List.of(), 0, false, null));

        AgentToolResult result = tool.searchObjects(
                new ObjectSearchQuery("%user%", null, null, null, null),
                InvocationParameters.from(Map.of(
                        InvocationContextConstant.CONNECTION_ID, "5",
                        InvocationContextConstant.AGENT_TYPE, AgentTypeEnum.EXPLORER.getCode(),
                        InvocationContextConstant.ALLOWED_CONNECTION_IDS, "5,7"
                ))
        );

        assertTrue(result.isSuccess());
        assertTrue(result.getElapsedMs() != null && result.getElapsedMs() >= 0);
        verify(discoveryService).searchObjects("%user%", null, null, null, null);
    }

    @Test
    void explorerMode_withDatabaseScope_usesDefaultExplorerConnection() {
        when(discoveryService.searchObjects("%user%", null, 5L, "app", null))
                .thenReturn(new ObjectSearchResponse(List.of(), 0, false, null));

        AgentToolResult result = tool.searchObjects(
                new ObjectSearchQuery("%user%", null, null, "app", null),
                InvocationParameters.from(Map.of(
                        InvocationContextConstant.CONNECTION_ID, "5",
                        InvocationContextConstant.AGENT_TYPE, AgentTypeEnum.EXPLORER.getCode(),
                        InvocationContextConstant.ALLOWED_CONNECTION_IDS, "5,7"
                ))
        );

        assertTrue(result.isSuccess());
        assertTrue(result.getElapsedMs() != null && result.getElapsedMs() >= 0);
        verify(discoveryService).searchObjects("%user%", null, 5L, "app", null);
    }

    @Test
    void explorerMode_rejectsOutOfScopeConnectionId() {
        AgentToolResult result = tool.searchObjects(
                new ObjectSearchQuery("%user%", null, 9L, null, null),
                InvocationParameters.from(Map.of(
                        InvocationContextConstant.CONNECTION_ID, "5",
                        InvocationContextConstant.AGENT_TYPE, AgentTypeEnum.EXPLORER.getCode(),
                        InvocationContextConstant.ALLOWED_CONNECTION_IDS, "5,7"
                ))
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("not allowed"));
        verifyNoInteractions(discoveryService);
    }

    @Test
    void returnsBlockingMessageWhenSearchHasResultsAndScopeErrors() {
        when(discoveryService.searchObjects("%user%", null, null, null, null))
                .thenReturn(new ObjectSearchResponse(
                        List.of(new ObjectSearchResult(1L, "test1", "mysql", "app", "public", "users", "TABLE")),
                        1,
                        false,
                        List.of("connectionId=7 timeout")
                ));

        AgentToolResult result = tool.searchObjects(
                new ObjectSearchQuery("%user%", null, null, null, null),
                InvocationParameters.from(Map.of())
        );

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Object search encountered scope failures"));
        assertTrue(result.getMessage().contains("pattern=%user%"));
        assertTrue(result.getMessage().contains("connectionId=7 timeout"));
        assertTrue(result.getMessage().contains("Use askUserQuestion to ask the user to clarify the target scope before continuing."));
    }

    @Test
    void returnsBlockingMessageWhenSearchFailsForScopeWithoutResults() {
        when(discoveryService.searchObjects("%user%", null, 5L, null, null))
                .thenReturn(new ObjectSearchResponse(List.of(), 0, false, List.of("connectionId=5 closed")));

        AgentToolResult result = tool.searchObjects(
                new ObjectSearchQuery("%user%", null, 5L, null, null),
                InvocationParameters.from(Map.of(InvocationContextConstant.CONNECTION_ID, "5"))
        );

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Object search encountered scope failures"));
        assertTrue(result.getMessage().contains("pattern=%user%, connectionId=5"));
        assertTrue(result.getMessage().contains("connectionId=5 closed"));
        assertTrue(result.getMessage().contains("Use askUserQuestion to ask the user to clarify the target scope before continuing."));
    }

    @Test
    void returnsNarrowingMessageWhenSingleCandidateMatches() {
        when(discoveryService.searchObjects("chat2db_user", DatabaseObjectTypeEnum.TABLE, 3L, "enterprise_gateway_dev", null))
                .thenReturn(new ObjectSearchResponse(List.of(
                        new ObjectSearchResult(3L, "test3", "mysql", "enterprise_gateway_dev", null, "chat2db_user", "TABLE")
                ), 1, false, null));

        AgentToolResult result = tool.searchObjects(
                new ObjectSearchQuery("chat2db_user", "TABLE", 3L, "enterprise_gateway_dev", null),
                InvocationParameters.from(Map.of())
        );

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Object search found 1 candidate(s)"));
        assertTrue(result.getMessage().contains("Use askUserQuestion to ask the user to narrow the target object before continuing."));
    }

    @Test
    void returnsExpensiveFuzzySearchGuidanceWhenMultipleCandidatesMatch() {
        List<ObjectSearchResult> candidates = IntStream.range(0, 2)
                .mapToObj(index -> new ObjectSearchResult(
                        3L,
                        "test3",
                        "mysql",
                        "enterprise_gateway_dev",
                        null,
                        "user_table_" + index,
                        "TABLE"
                ))
                .toList();
        when(discoveryService.searchObjects("%user%", DatabaseObjectTypeEnum.TABLE, 3L, "enterprise_gateway_dev", null))
                .thenReturn(new ObjectSearchResponse(candidates, 2, false, null));

        AgentToolResult result = tool.searchObjects(
                new ObjectSearchQuery("%user%", "TABLE", 3L, "enterprise_gateway_dev", null),
                InvocationParameters.from(Map.of())
        );

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Object search found 2 candidate(s)"));
        assertTrue(result.getMessage().contains("Use askUserQuestion to ask the user to narrow the target object before continuing."));
    }

    private SearchObjectsTool proxiedTool() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new SearchObjectsTool(discoveryService));
        factory.addAspect(new AgentToolContextAspect(new ToolErrorMapper()));
        return factory.getProxy();
    }
}
