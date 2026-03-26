package edu.zsc.ai.agent.tool.export;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.export.model.ExportRowInput;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.service.ai.export.FileExportService;
import edu.zsc.ai.domain.service.ai.export.model.ExportedFilePayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExportFileToolTest {

    private final FileExportService exportFileService = mock(FileExportService.class);
    private final ExportFileTool tool = new ExportFileTool(exportFileService);

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void exportFileRequiresUserContext() {
        AgentToolResult result = tool.exportFile(
                "CSV",
                List.of("name"),
                List.of(new ExportRowInput(List.<Object>of("Alice"))),
                InvocationParameters.from(Map.of())
        );
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("user session context is not available"));
    }

    @Test
    void exportFileReturnsSuccessPayloadWhenContextIsAvailable() {
        RequestContext.set(RequestContextInfo.builder()
                .userId(42L)
                .conversationId(7L)
                .build());
        when(exportFileService.export(any())).thenReturn(ExportedFilePayload.builder()
                .fileId("f-1")
                .filename("20260325123456000-42-7-f-1.csv")
                .format("CSV")
                .sizeBytes(12)
                .downloadPath("/api/ai/files/f-1")
                .build());

        AgentToolResult result = tool.exportFile(
                "CSV",
                List.of("name"),
                List.of(new ExportRowInput(List.<Object>of("Alice"))),
                InvocationParameters.from(Map.of())
        );
        assertTrue(result.isSuccess());
        assertTrue(String.valueOf(result.getResult()).contains("/api/ai/files/f-1"));
    }
}
