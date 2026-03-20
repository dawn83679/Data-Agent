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
        assertTrue(result.getMessage().contains("Environment overview is available for 2 connection(s)."));
        assertTrue(result.getMessage().contains("current instruction, mention, connectionId, catalog, or schema"));
        assertTrue(result.getMessage().contains("continue within that scope instead of asking the user to reconfirm it"));
    }

    @Test
    void returnsUnavailableConnectionsInMessageWhenSomeConnectionsFail() {
        when(discoveryService.getEnvironmentOverview()).thenReturn(List.of(
                new ConnectionOverview(1L, "test1", "mysql", List.of(new CatalogInfo("db1", List.of())), null),
                new ConnectionOverview(2L, "test2", "mysql", List.of(), "Connection unreachable or error: timeout")
        ));

        AgentToolResult result = tool.getEnvironmentOverview(InvocationParameters.from(Map.of()));

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Environment overview is only partially available."));
        assertTrue(result.getMessage().contains("test2(id=2): Connection unreachable or error: timeout"));
        assertTrue(result.getMessage().contains("Continue with the remaining available connections."));
        assertTrue(result.getMessage().contains("only when the task still depends on an unavailable connection"));
    }

    @Test
    void returnsUserEscalationMessageWhenAllConnectionsFail() {
        when(discoveryService.getEnvironmentOverview()).thenReturn(List.of(
                new ConnectionOverview(3L, "test3", "mysql", List.of(), "Connection unreachable or error: connection closed")
        ));

        AgentToolResult result = tool.getEnvironmentOverview(InvocationParameters.from(Map.of()));

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Environment overview could not find any usable connection."));
        assertTrue(result.getMessage().contains("test3(id=3): Connection unreachable or error: connection closed"));
        assertTrue(result.getMessage().contains("Ask the user whether to retry later or check the connection configuration"));
        assertTrue(result.getMessage().contains("Do not continue object discovery until a usable connection is available"));
    }

    @Test
    void returnsChineseMessageWhenInvocationLanguageIsChinese() {
        when(discoveryService.getEnvironmentOverview()).thenReturn(List.of(
                new ConnectionOverview(1L, "test1", "mysql", List.of(new CatalogInfo("db1", List.of())), null)
        ));

        AgentToolResult result = tool.getEnvironmentOverview(InvocationParameters.from(Map.of("language", "zh")));

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("当前环境中有 1 个可用连接"));
        assertTrue(result.getMessage().contains("只有当当前指令、mention、connectionId、catalog 或 schema 仍不足以唯一定位目标时"));
    }
}
