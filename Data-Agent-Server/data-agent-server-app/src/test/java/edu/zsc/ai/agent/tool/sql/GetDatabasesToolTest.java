package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.domain.service.db.DatabaseService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetDatabasesToolTest {

    private final DatabaseService databaseService = mock(DatabaseService.class);
    private final GetDatabasesTool tool = new GetDatabasesTool(databaseService);

    @Test
    void returnsDatabasesAndAsksUserToChooseDatabase() {
        when(databaseService.getDatabases(8L)).thenReturn(List.of("app_core", "analytics"));

        AgentToolResult result = tool.getDatabases(8L, InvocationParameters.from(Map.of()));

        assertTrue(result.isSuccess());
        assertThat(result.getResult()).isEqualTo(List.of("app_core", "analytics"));
        assertThat(result.getMessage()).contains("Found 2 database(s) on connection 8.");
        assertThat(result.getMessage()).contains("Use askUserQuestion to ask the user which database should be used before continuing.");
    }

    @Test
    void returnsEmptyAndAsksUserWhetherAnotherConnectionShouldBeUsed() {
        when(databaseService.getDatabases(8L)).thenReturn(List.of());

        AgentToolResult result = tool.getDatabases(8L, InvocationParameters.from(Map.of()));

        assertTrue(result.isSuccess());
        assertThat(result.getResult()).isNull();
        assertThat(result.getMessage()).contains("No databases found on connection 8.");
        assertThat(result.getMessage()).contains("Use askUserQuestion to ask the user whether another connection should be used before continuing.");
    }

    @Test
    void failsAndAsksUserWhetherToVerifyOrTryAnotherConnection() {
        when(databaseService.getDatabases(8L)).thenThrow(new IllegalStateException("timeout"));

        AgentToolResult result = tool.getDatabases(8L, InvocationParameters.from(Map.of()));

        assertFalse(result.isSuccess());
        assertThat(result.getMessage()).contains("Failed to get databases on connection 8: timeout");
        assertThat(result.getMessage()).contains("Use askUserQuestion to ask the user whether to verify the connection or try another connection before continuing.");
    }
}
