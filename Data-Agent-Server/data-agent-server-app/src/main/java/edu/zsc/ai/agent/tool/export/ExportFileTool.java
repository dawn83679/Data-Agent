package edu.zsc.ai.agent.tool.export;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.annotation.DisallowInPlanMode;
import edu.zsc.ai.agent.tool.ToolDescriptionParam;
import edu.zsc.ai.agent.tool.export.model.ExportRowInput;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.service.ai.export.FileExportService;
import edu.zsc.ai.domain.service.ai.export.model.ExportedFilePayload;
import edu.zsc.ai.domain.service.ai.export.model.FileExportRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@AgentTool
@Slf4j
@RequiredArgsConstructor
public class ExportFileTool {

    private final FileExportService exportFileService;

    @Tool({
            "Value: turns prepared tabular data into a downloadable file for the user.",
            "Use When: verified tabular data is ready and the user wants a file.",
            "Preconditions: format must be supported, headers must be present, and every row must match the header column count.",
            "Result: downloadable file card.",
            "Boundary: supported formats are CSV, XLSX, DOCX, and PDF; do not export unverified data."
    })
    @DisallowInPlanMode(ToolNameEnum.EXPORT_FILE)
    public AgentToolResult exportFile(
            @P("Export format. Supported values: CSV, XLSX, DOCX, PDF.") String format,
            @P("Column headers for the exported table.") List<String> headers,
            @P("Table rows shaped as { cells: [...] }; each row must match the header count.")
            List<ExportRowInput> rows,
            @P(value = ToolDescriptionParam.UI_STEP_DESCRIPTION, required = false) String description,
            InvocationParameters parameters) {
        Long userId = RequestContext.getUserId();
        if (userId == null) {
            return AgentToolResult.noContext();
        }

        ExportedFilePayload payload = exportFileService.export(FileExportRequest.builder()
                .format(format)
                .headers(headers)
                .rows(rows == null ? null : rows.stream().map(row -> row == null ? null : row.getCells()).toList())
                .userId(userId)
                .conversationId(RequestContext.getConversationId())
                .build());

        log.info("[Tool done] exportFile, fileId={}, format={}, sizeBytes={}",
                payload.getFileId(), payload.getFormat(), payload.getSizeBytes());
        return AgentToolResult.success(payload, ToolMessageSupport.sentence(
                "Export file is ready for download.",
                "Use the returned file card as the delivery artifact and keep any follow-up explanation consistent with the preview."
        ));
    }

    public AgentToolResult exportFile(
            String format,
            List<String> headers,
            List<ExportRowInput> rows,
            InvocationParameters parameters) {
        return exportFile(format, headers, rows, null, parameters);
    }
}
