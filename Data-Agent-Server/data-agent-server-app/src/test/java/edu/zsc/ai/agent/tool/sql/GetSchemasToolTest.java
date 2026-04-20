package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.domain.service.db.SchemaService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetSchemasToolTest {

    private final SchemaService schemaService = mock(SchemaService.class);
    private final GetSchemasTool tool = new GetSchemasTool(schemaService);

    @Test
    void returnsSchemasAndAsksUserToChooseSchema() {
        when(schemaService.listSchemas(8L, "app_core")).thenReturn(List.of("public", "audit"));

        AgentToolResult result = tool.getSchemas(8L, "app_core", InvocationParameters.from(Map.of()));

        assertTrue(result.isSuccess());
        assertThat(result.getResult()).isEqualTo(List.of("public", "audit"));
        assertThat(result.getMessage()).contains("Found 2 schema(s) in database 'app_core' on connection 8.");
        assertThat(result.getMessage()).contains("Use askUserQuestion to ask the user which schema should be used before continuing.");
    }

    @Test
    void returnsEmptyAndAsksWhetherAnotherDatabaseShouldBeUsedOrSchemaCanBeSkipped() {
        when(schemaService.listSchemas(8L, "app_core")).thenReturn(List.of());

        AgentToolResult result = tool.getSchemas(8L, "app_core", InvocationParameters.from(Map.of()));

        assertTrue(result.isSuccess());
        assertThat(result.getResult()).isNull();
        assertThat(result.getMessage()).contains("No schemas found in database 'app_core' on connection 8.");
        assertThat(result.getMessage()).contains("Use askUserQuestion to ask the user whether another database should be used or whether schema scope can be skipped before continuing.");
    }

    @Test
    void failsAndAsksWhetherToRetryWithAnotherDatabaseOrConnection() {
        when(schemaService.listSchemas(8L, "app_core")).thenThrow(new IllegalStateException("closed"));

        AgentToolResult result = tool.getSchemas(8L, "app_core", InvocationParameters.from(Map.of()));

        assertFalse(result.isSuccess());
        assertThat(result.getMessage()).contains("Failed to get schemas in database 'app_core' on connection 8: closed");
        assertThat(result.getMessage()).contains("Use askUserQuestion to ask the user whether to retry with another database or connection before continuing.");
    }
}
