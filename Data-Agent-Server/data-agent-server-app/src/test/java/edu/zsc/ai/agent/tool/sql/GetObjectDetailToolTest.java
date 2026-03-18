package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.sql.model.NamedObjectDetail;
import edu.zsc.ai.agent.tool.sql.model.ObjectDetail;
import edu.zsc.ai.agent.tool.sql.model.ObjectQueryItem;
import edu.zsc.ai.domain.service.db.DiscoveryService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetObjectDetailToolTest {

    private final DiscoveryService discoveryService = mock(DiscoveryService.class);
    private final GetObjectDetailTool tool = new GetObjectDetailTool(discoveryService);

    @Test
    void returnsSuccessMessageWhenAllObjectDetailsSucceed() {
        when(discoveryService.getObjectDetails(List.of(new ObjectQueryItem("TABLE", "users", 1L, "app", "public"))))
                .thenReturn(List.of(
                        new NamedObjectDetail("users", "TABLE", true, null, new ObjectDetail("ddl", 10L, List.of()))
                ));

        AgentToolResult result = tool.getObjectDetail(
                List.of(new ObjectQueryItem("TABLE", "users", 1L, "app", "public")),
                InvocationParameters.from(Map.of())
        );

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Object details are available for users"));
        assertTrue(result.getMessage().contains("Use the returned DDL, row counts, and indexes"));
    }

    @Test
    void returnsBlockingMessageWhenSomeObjectDetailsFail() {
        List<ObjectQueryItem> objects = List.of(
                new ObjectQueryItem("TABLE", "users", 1L, "app", "public"),
                new ObjectQueryItem("TABLE", "orders_archive", 1L, "app", "public")
        );
        when(discoveryService.getObjectDetails(objects))
                .thenReturn(List.of(
                        new NamedObjectDetail("users", "TABLE", true, null, new ObjectDetail("ddl", 10L, List.of())),
                        new NamedObjectDetail("orders_archive", "TABLE", false, "connection closed", null)
                ));

        AgentToolResult result = tool.getObjectDetail(objects, InvocationParameters.from(Map.of()));

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Object detail lookup is only partially available."));
        assertTrue(result.getMessage().contains("orders_archive (connection closed)"));
        assertTrue(result.getMessage().contains("Do not assume the structure of failed objects"));
        assertTrue(result.getMessage().contains("Ask the user whether these objects are still required before continuing"));
        assertTrue(result.getMessage().contains("Do not continue object discovery until the user replies"));
    }

    @Test
    void returnsBlockingMessageWhenAllObjectDetailsFail() {
        List<ObjectQueryItem> objects = List.of(
                new ObjectQueryItem("TABLE", "users_backup", 1L, "app", "public")
        );
        when(discoveryService.getObjectDetails(objects))
                .thenReturn(List.of(
                        new NamedObjectDetail("users_backup", "TABLE", false, "timeout", null)
                ));

        AgentToolResult result = tool.getObjectDetail(objects, InvocationParameters.from(Map.of()));

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Object detail lookup failed for all requested objects: users_backup (timeout)"));
        assertTrue(result.getMessage().contains("Ask the user whether to retry with another object or connection"));
        assertTrue(result.getMessage().contains("Do not continue object discovery until the user replies"));
    }
}
