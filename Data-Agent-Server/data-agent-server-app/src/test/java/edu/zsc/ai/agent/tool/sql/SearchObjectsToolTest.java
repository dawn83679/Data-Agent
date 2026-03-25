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
        assertTrue(result.getMessage().contains("Object search returned partial results"));
        assertTrue(result.getMessage().contains("pattern=%user%"));
        assertTrue(result.getMessage().contains("connectionId=7 timeout"));
        assertTrue(result.getMessage().contains("Continue only with the currently returned matches"));
        assertTrue(result.getMessage().contains("Ask the user whether to keep these matches or adjust the connection or scope"));
        assertTrue(result.getMessage().contains("Do not continue object discovery until the user replies"));
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
        assertTrue(result.getMessage().contains("Object search could not return reliable matches"));
        assertTrue(result.getMessage().contains("pattern=%user%, connectionId=5"));
        assertTrue(result.getMessage().contains("connectionId=5 closed"));
        assertTrue(result.getMessage().contains("Ask the user whether to retry with another connection or scope"));
        assertTrue(result.getMessage().contains("Do not continue object discovery until the user replies"));
    }

    @Test
    void returnsExpensiveFuzzySearchGuidanceWhenManyCandidatesMatch() {
        List<ObjectSearchResult> candidates = IntStream.range(0, 18)
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
                .thenReturn(new ObjectSearchResponse(candidates, 18, false, null));

        AgentToolResult result = tool.searchObjects(
                new ObjectSearchQuery("%user%", "TABLE", 3L, "enterprise_gateway_dev", null),
                InvocationParameters.from(Map.of())
        );

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("broad fuzzy search result"));
        assertTrue(result.getMessage().contains("Broad fuzzy discovery over many similarly named objects is expensive."));
        assertTrue(result.getMessage().contains("Prefer askUserQuestion to confirm the intended table name, object scope, or expected result shape"));
    }

    private SearchObjectsTool proxiedTool() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new SearchObjectsTool(discoveryService));
        factory.addAspect(new AgentToolContextAspect(new ToolErrorMapper()));
        return factory.getProxy();
    }
}
