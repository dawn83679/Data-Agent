package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.annotation.AgentTool;
import edu.zsc.ai.agent.tool.model.AgentConnectionView;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionResponse;
import edu.zsc.ai.domain.service.db.DbConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

@AgentTool
@Slf4j
@RequiredArgsConstructor
public class ConnectionTool {

    private final DbConnectionService dbConnectionService;

    @Tool({
        "[GOAL] List all available database connections for the current user.",
        "[WHEN] Use when connectionId is unknown, multiple data sources may match, or user asks to switch source.",
        "[WHEN_NOT] Do not call if connectionId is already known from session context. Do not use to list databases — use getCatalogNames."
    })
    public AgentToolResult getConnections(InvocationParameters parameters) {
        log.info("[Tool] getConnections");
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }
            List<ConnectionResponse> connections = dbConnectionService.getAllConnections(userId);
            if (Objects.isNull(connections) || connections.isEmpty()) {
                log.info("[Tool done] getConnections -> empty");
                return AgentToolResult.empty();
            }
            log.info("[Tool done] getConnections, result size={}", connections.size());
            return AgentToolResult.success(AgentConnectionView.fromList(connections));
        } catch (Exception e) {
            log.error("[Tool error] getConnections", e);
            return AgentToolResult.fail("Failed to list connections: " + e.getMessage());
        }
    }
}
