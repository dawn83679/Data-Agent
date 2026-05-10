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
            "价值：把已准备好的表格数据导出成用户可下载的文件。",
            "使用时机：表格数据已验证，且用户需要文件。",
            "前置条件：format 必须受支持，headers 必须存在，每行单元格数量必须匹配表头列数。",
            "结果：下载文件卡片就是本轮最终交付物。",
            "边界：支持 CSV、XLSX、DOCX、PDF；成功后不要再输出助手文本。"
    })
    @DisallowInPlanMode(ToolNameEnum.EXPORT_FILE)
    public AgentToolResult exportFile(
            @P("导出格式，支持 CSV、XLSX、DOCX、PDF。") String format,
            @P("导出表格的列头。") List<String> headers,
            @P("表格行，格式为 { cells: [...] }；每行单元格数量必须匹配表头列数。")
            List<ExportRowInput> rows,
            @P(ToolDescriptionParam.UI_STEP_DESCRIPTION) String description,
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
                "导出文件已可下载。",
                "文件卡片就是本轮最终答案。立即结束本轮，不要在工具调用后输出任何描述、总结或评论导出文件的助手文本。"
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
