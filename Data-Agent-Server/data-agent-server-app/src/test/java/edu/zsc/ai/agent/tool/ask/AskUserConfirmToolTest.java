package edu.zsc.ai.agent.tool.ask;

import edu.zsc.ai.agent.tool.ask.confirm.WriteConfirmationEntry;
import edu.zsc.ai.agent.tool.ask.confirm.WriteConfirmationStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AskUserConfirmToolTest {

    private final WriteConfirmationStore confirmationStore = mock(WriteConfirmationStore.class);
    private final AskUserConfirmTool tool = new AskUserConfirmTool(confirmationStore);

    @Test
    void askUserConfirm_returnsTokenAndDisplayContext() {
        when(confirmationStore.create(any(), eq("DELETE FROM orders WHERE id = 1")))
                .thenReturn(WriteConfirmationEntry.builder()
                        .token("token-123")
                        .connectionId(5L)
                        .catalog("sales")
                        .schema("public")
                        .sql("DELETE FROM orders WHERE id = 1")
                        .build());

        AskUserConfirmTool.WriteConfirmationResult result = tool.askUserConfirm(
                "DELETE FROM orders WHERE id = 1",
                5L,
                "sales",
                "public",
                "Delete one order row",
                null
        );

        assertEquals("token-123", result.getConfirmationToken());
        assertEquals("DELETE FROM orders WHERE id = 1", result.getSqlPreview());
        assertEquals("Delete one order row", result.getExplanation());
        assertEquals(5L, result.getConnectionId());
        assertEquals("sales", result.getDatabaseName());
        assertEquals("public", result.getSchemaName());
        assertEquals(300L, result.getExpiresInSeconds());
    }
}
