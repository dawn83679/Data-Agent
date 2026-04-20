package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.sql.model.AvailableConnectionItem;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionResponse;
import edu.zsc.ai.domain.service.db.DbConnectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetConnectionsToolTest {

    private final DbConnectionService connectionService = mock(DbConnectionService.class);
    private final GetConnectionsTool tool = new GetConnectionsTool(connectionService);

    @BeforeEach
    void setUp() {
        // no setup required
    }

    @Test
    void returnsAvailableConnectionItems() {
        when(connectionService.getAllConnections()).thenReturn(List.of(
                ConnectionResponse.builder().id(1L).name("primary").dbType("mysql").host("10.0.0.1").username("root").build(),
                ConnectionResponse.builder().id(2L).name("analytics").dbType("postgres").build()
        ));

        AgentToolResult result = tool.getConnections(InvocationParameters.from(Map.of()));

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        List<AvailableConnectionItem> items = assertInstanceOf(List.class, result.getResult());
        assertThat(items).hasSize(2);
        assertThat(items).extracting(AvailableConnectionItem::id).containsExactly(1L, 2L);
        assertThat(items).extracting(AvailableConnectionItem::name).containsExactly("primary", "analytics");
        assertThat(items).extracting(AvailableConnectionItem::dbType).containsExactly("mysql", "postgres");
        assertThat(result.getMessage()).doesNotContain("10.0.0.1");
        assertThat(result.getMessage()).doesNotContain("root");
        assertThat(result.getMessage()).contains("askUserQuestion");
        assertThat(result.getMessage()).contains("which connection");
    }

    @Test
    void returnsEmptyWhenNoConnections() {
        when(connectionService.getAllConnections()).thenReturn(List.of());

        AgentToolResult result = tool.getConnections(InvocationParameters.from(Map.of()));

        assertTrue(result.isSuccess());
        assertThat(result.getResult()).isNull();
        assertThat(result.getMessage()).contains("No available connections were found");
    }

    @Test
    void failsWhenServiceThrows() {
        when(connectionService.getAllConnections()).thenThrow(new IllegalStateException("no user"));

        AgentToolResult result = tool.getConnections(InvocationParameters.from(Map.of()));

        assertFalse(result.isSuccess());
        assertThat(result.getMessage()).contains("Failed to list available connections");
    }
}
