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
import edu.zsc.ai.domain.service.db.DatabaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AgentTool
@Slf4j
@Component
@RequiredArgsConstructor
public class GetDatabasesTool {

    private final DatabaseService databaseService;

    @Tool({
            "价值：返回指定连接上的数据库或 catalog 列表。",
            "使用时机：连接已确定，但数据库或 catalog 范围缺失。",
            "前置条件：connectionId 必填。",
            "结果：该连接上的数据库名称列表。",
            "边界：范围已经明确时不要重复调用它重新发现范围。"
    })
    public AgentToolResult getDatabases(
            @P("要查询数据库列表的连接 ID") Long connectionId,
            @P(ToolDescriptionParam.UI_STEP_DESCRIPTION) String description,
            InvocationParameters parameters) {
        log.info("[Tool] getDatabases connectionId={}", connectionId);
        try {
            List<String> databases = databaseService.getDatabases(connectionId);
            if (CollectionUtils.isEmpty(databases)) {
                log.info("[Tool done] getDatabases -> empty");
                return AgentToolResult.empty(buildEmptyMessage(connectionId));
            }
            log.info("[Tool done] getDatabases connectionId={}, count={}", connectionId, databases.size());
            return AgentToolResult.success(databases, buildSuccessMessage(connectionId, databases.size()));
        } catch (Exception e) {
            log.warn("[Tool] getDatabases failed for connectionId={}: {}", connectionId, e.getMessage());
            return AgentToolResult.fail(buildFailureMessage(connectionId, e.getMessage()));
        }
    }

    public AgentToolResult getDatabases(Long connectionId, InvocationParameters parameters) {
        return getDatabases(connectionId, null, parameters);
    }

    private String buildSuccessMessage(Long connectionId, int databaseCount) {
        return ToolMessageSupport.sentence(
                "连接 " + connectionId + " 上找到 " + databaseCount + " 个数据库。",
                "继续前使用 askUserQuestion 询问用户要使用哪个数据库。"
        );
    }

    private String buildEmptyMessage(Long connectionId) {
        return ToolMessageSupport.sentence(
                "连接 " + connectionId + " 上没有找到数据库。",
                "继续前使用 askUserQuestion 询问用户是否要改用其他连接。"
        );
    }

    private String buildFailureMessage(Long connectionId, String errorMessage) {
        return ToolMessageSupport.sentence(
                "获取连接 " + connectionId + " 上的数据库失败：" + errorMessage + "。",
                "继续前使用 askUserQuestion 询问用户是否要检查连接或尝试其他连接。"
        );
    }
}
