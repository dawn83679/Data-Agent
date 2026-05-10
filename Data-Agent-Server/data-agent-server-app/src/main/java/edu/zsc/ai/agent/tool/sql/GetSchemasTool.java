package edu.zsc.ai.agent.tool.sql;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ToolDescriptionParam;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.domain.service.db.SchemaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AgentTool
@Slf4j
@Component
@RequiredArgsConstructor
public class GetSchemasTool {

    private final SchemaService schemaService;

    @Tool({
            "价值：返回指定连接和数据库下的 schema 列表。",
            "使用时机：连接和数据库已确定，但 schema 范围缺失。",
            "前置条件：connectionId 和 databaseName 必填。",
            "结果：该数据库下的 schema 名称列表；不支持 schema 的数据库类型可能返回空。",
            "边界：根据数据库类型判断是否需要 schema 范围，不要机械追问不存在的层级。"
    })
    public AgentToolResult getSchemas(
            @P("连接 ID") Long connectionId,
            @P("数据库或 catalog 名称") String databaseName,
            @P(ToolDescriptionParam.UI_STEP_DESCRIPTION) String description,
            InvocationParameters parameters) {
        log.info("[Tool] getSchemas connectionId={}, database={}", connectionId, databaseName);
        try {
            List<String> schemas = schemaService.listSchemas(connectionId, databaseName);
            if (CollectionUtils.isEmpty(schemas)) {
                log.info("[Tool done] getSchemas -> empty");
                return AgentToolResult.empty(buildEmptyMessage(connectionId, databaseName));
            }
            log.info("[Tool done] getSchemas connectionId={}, database={}, count={}",
                    connectionId, databaseName, schemas.size());
            return AgentToolResult.success(schemas, buildSuccessMessage(connectionId, databaseName, schemas.size()));
        } catch (Exception e) {
            log.warn("[Tool] getSchemas failed for connectionId={}, database={}: {}",
                    connectionId, databaseName, e.getMessage());
            return AgentToolResult.fail(buildFailureMessage(connectionId, databaseName, e.getMessage()));
        }
    }

    public AgentToolResult getSchemas(Long connectionId, String databaseName, InvocationParameters parameters) {
        return getSchemas(connectionId, databaseName, null, parameters);
    }

    private String buildSuccessMessage(Long connectionId, String databaseName, int schemaCount) {
        return ToolMessageSupport.sentence(
                "连接 " + connectionId + " 的数据库 `" + databaseName + "` 中找到 " + schemaCount + " 个 schema。",
                "继续前使用 askUserQuestion 询问用户要使用哪个 schema。"
        );
    }

    private String buildEmptyMessage(Long connectionId, String databaseName) {
        return ToolMessageSupport.sentence(
                "连接 " + connectionId + " 的数据库 `" + databaseName + "` 中没有找到 schema。",
                "继续前使用 askUserQuestion 询问用户是否要改用其他数据库，或该数据库类型是否可以跳过 schema 范围。"
        );
    }

    private String buildFailureMessage(Long connectionId, String databaseName, String errorMessage) {
        return ToolMessageSupport.sentence(
                "获取连接 " + connectionId + " 的数据库 `" + databaseName + "` 下的 schema 失败：" + errorMessage + "。",
                "继续前使用 askUserQuestion 询问用户是否要改用其他数据库或连接。"
        );
    }
}
