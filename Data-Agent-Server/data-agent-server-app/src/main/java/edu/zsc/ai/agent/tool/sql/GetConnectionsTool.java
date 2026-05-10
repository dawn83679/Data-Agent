package edu.zsc.ai.agent.tool.sql;

import java.util.List;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ToolDescriptionParam;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.sql.model.AvailableConnectionItem;
import edu.zsc.ai.domain.service.db.DbConnectionService;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@AgentTool
@Slf4j
@Component
@RequiredArgsConstructor
public class GetConnectionsTool {

    private final DbConnectionService dbConnectionService;

    @Tool({
            "价值：返回当前用户可用的数据源连接列表。",
            "使用时机：连接范围缺失，或用户询问有哪些连接可用。",
            "前置条件：无。",
            "结果：每项只包含 id、name 和 dbType。",
            "边界：不要编造主机、端口、凭据或隐藏元数据。"
    })
    public AgentToolResult getConnections(
            @P(ToolDescriptionParam.UI_STEP_DESCRIPTION) String description,
            InvocationParameters parameters) {
        log.info("[Tool] getConnections");
        try {
            List<AvailableConnectionItem> connections = dbConnectionService.getAllConnections().stream()
                    .map(this::mapToAvailableConnectionItem)
                    .toList();
            if (connections.isEmpty()) {
                log.info("[Tool done] getConnections -> empty");
                return AgentToolResult.empty("当前会话没有找到可用连接。");
            }
            log.info("[Tool done] getConnections count={}", connections.size());
            return AgentToolResult.success(connections,
                    ToolMessageSupport.sentence(
                            "已返回当前会话的 " + connections.size() + " 个可用连接。",
                            "如果当前任务仍缺少明确连接范围，继续前使用 askUserQuestion 询问用户要使用哪个连接。"
                    ));
        } catch (Exception e) {
            log.warn("[Tool] getConnections failed: {}", e.getMessage());
            return AgentToolResult.fail(
                    "获取可用连接失败：" + e.getMessage());
        }
    }

    public AgentToolResult getConnections(InvocationParameters parameters) {
        return getConnections(null, parameters);
    }

    private AvailableConnectionItem mapToAvailableConnectionItem(ConnectionResponse connection) {
        return new AvailableConnectionItem(connection.getId(), connection.getName(), connection.getDbType());
    }
}
