package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.sql.model.CatalogInfo;
import edu.zsc.ai.agent.tool.sql.model.ConnectionOverview;
import edu.zsc.ai.domain.service.db.DiscoveryService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetEnvironmentOverviewToolTest {

    private final DiscoveryService discoveryService = mock(DiscoveryService.class);
    private final GetEnvironmentOverviewTool tool = new GetEnvironmentOverviewTool(discoveryService);

    @Test
    void returnsAvailabilitySummaryWhenAllConnectionsAreReachable() {
        when(discoveryService.getEnvironmentOverview()).thenReturn(List.of(
                new ConnectionOverview(1L, "test1", "mysql", List.of(new CatalogInfo("db1", List.of())), null),
                new ConnectionOverview(2L, "test2", "mysql", List.of(new CatalogInfo("db2", List.of())), null)
        ));

        AgentToolResult result = tool.getEnvironmentOverview(InvocationParameters.from(Map.of()));

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Environment overview found 2 usable connection(s)."));
        assertTrue(result.getMessage().contains("Use askUserQuestion to ask the user to narrow the search scope before continuing."));
    }

    @Test
    void returnsUnavailableConnectionsInMessageWhenSomeConnectionsFail() {
        when(discoveryService.getEnvironmentOverview()).thenReturn(List.of(
                new ConnectionOverview(1L, "test1", "mysql", List.of(new CatalogInfo("db1", List.of())), null),
                new ConnectionOverview(2L, "test2", "mysql", List.of(), "Connection unreachable or error: timeout")
        ));

        AgentToolResult result = tool.getEnvironmentOverview(InvocationParameters.from(Map.of()));

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Environment overview found 1 usable connection(s)."));
        assertTrue(result.getMessage().contains("test2(id=2): Connection unreachable or error: timeout"));
        assertTrue(result.getMessage().contains("Use askUserQuestion to ask the user to narrow the search scope before continuing."));
    }

    @Test
    void returnsUserEscalationMessageWhenAllConnectionsFail() {
        when(discoveryService.getEnvironmentOverview()).thenReturn(List.of(
                new ConnectionOverview(3L, "test3", "mysql", List.of(), "Connection unreachable or error: connection closed")
        ));

        AgentToolResult result = tool.getEnvironmentOverview(InvocationParameters.from(Map.of()));

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Environment overview returned no usable connections."));
        assertTrue(result.getMessage().contains("Use askUserQuestion to ask the user whether to retry later or check the connection configuration."));
    }

    @Test
    void stillReturnsEnglishMessageWhenInvocationLanguageIsChinese() {
        when(discoveryService.getEnvironmentOverview()).thenReturn(List.of(
                new ConnectionOverview(1L, "test1", "mysql", List.of(new CatalogInfo("db1", List.of())), null)
        ));

        AgentToolResult result = tool.getEnvironmentOverview(InvocationParameters.from(Map.of("language", "zh")));

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Environment overview found 1 usable connection(s)."));
        assertTrue(result.getMessage().contains("Use askUserQuestion to ask the user to narrow the search scope before continuing."));
    }

    @Test
    void returnsScopeClarificationMessageWhenReachableConnectionsExceedPreviousThreshold() {
        when(discoveryService.getEnvironmentOverview()).thenReturn(List.of(
                new ConnectionOverview(1L, "test1", "mysql", List.of(new CatalogInfo("db1", List.of())), null),
                new ConnectionOverview(2L, "test2", "mysql", List.of(new CatalogInfo("db2", List.of())), null),
                new ConnectionOverview(3L, "test3", "mysql", List.of(new CatalogInfo("db3", List.of())), null),
                new ConnectionOverview(4L, "test4", "mysql", List.of(new CatalogInfo("db4", List.of())), null)
        ));

        AgentToolResult result = tool.getEnvironmentOverview(InvocationParameters.from(Map.of()));

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Environment overview found 4 usable connection(s)."));
        assertTrue(result.getMessage().contains("Use askUserQuestion to ask the user to narrow the search scope before continuing."));
    }

    @Test
    void stillReturnsEnglishScopeClarificationMessageWhenInvocationLanguageIsChinese() {
        when(discoveryService.getEnvironmentOverview()).thenReturn(List.of(
                new ConnectionOverview(1L, "test1", "mysql", List.of(new CatalogInfo("db1", List.of())), null),
                new ConnectionOverview(2L, "test2", "mysql", List.of(new CatalogInfo("db2", List.of())), null),
                new ConnectionOverview(3L, "test3", "mysql", List.of(new CatalogInfo("db3", List.of())), null),
                new ConnectionOverview(4L, "test4", "mysql", List.of(new CatalogInfo("db4", List.of())), null)
        ));

        AgentToolResult result = tool.getEnvironmentOverview(InvocationParameters.from(Map.of("language", "zh")));

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Environment overview found 4 usable connection(s)."));
        assertTrue(result.getMessage().contains("Use askUserQuestion to ask the user to narrow the search scope before continuing."));
    }

    @Test
    void returnsScopeClarificationMessageWhenReachableConnectionsMeetPreviousThreshold() {
        when(discoveryService.getEnvironmentOverview()).thenReturn(List.of(
                new ConnectionOverview(1L, "test1", "mysql", List.of(new CatalogInfo("db1", List.of())), null),
                new ConnectionOverview(2L, "test2", "mysql", List.of(new CatalogInfo("db2", List.of())), null),
                new ConnectionOverview(3L, "test3", "mysql", List.of(new CatalogInfo("db3", List.of())), null)
        ));

        AgentToolResult result = tool.getEnvironmentOverview(InvocationParameters.from(Map.of("language", "zh")));

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Environment overview found 3 usable connection(s)."));
        assertTrue(result.getMessage().contains("Use askUserQuestion to ask the user to narrow the search scope before continuing."));
    }
}
