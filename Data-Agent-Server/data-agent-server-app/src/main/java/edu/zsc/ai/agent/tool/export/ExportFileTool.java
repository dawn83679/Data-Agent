package edu.zsc.ai.agent.tool.export;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.annotation.DisallowInPlanMode;
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
            "Use When: call when the data is already available and the user explicitly wants a file output instead of raw rows.",
            "Preconditions: format must be supported, headers must be present, and every row must match the header column count.",
            "After Success: treat the returned file card as the delivery artifact. Keep any narrative aligned with the preview and file metadata.",
            "After Failure: fix the export input or ask the user to narrow the output format before retrying.",
            "Do Not Use When: you do not yet have verified data or the user only needs a short textual answer.",
            "Relation: call activateSkill('file-export') before the first export in a session when that skill is available.",
            "Version: this environment currently supports CSV only."
    })
    @DisallowInPlanMode(ToolNameEnum.EXPORT_FILE)
    public AgentToolResult exportFile(
            @P("Export format. Currently only CSV is supported.") String format,
            @P(value = "Optional user-facing label for the export request. Does not override the final server filename.", required = false)
            String filename,
            @P("Column headers for the exported table.") List<String> headers,
            @P("Table rows. Provide a list of row objects, each shaped as { cells: [...] }. Every row must have the same number of cells as headers.")
            List<ExportRowInput> rows,
            InvocationParameters parameters) {
        Long userId = RequestContext.getUserId();
        if (userId == null) {
            return AgentToolResult.noContext();
        }

        ExportedFilePayload payload = exportFileService.export(FileExportRequest.builder()
                .format(format)
                .filename(filename)
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
}
