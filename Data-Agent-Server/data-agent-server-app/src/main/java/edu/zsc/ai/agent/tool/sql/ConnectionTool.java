package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
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

@AgentTool
@Slf4j
@RequiredArgsConstructor
public class ConnectionTool {

    private final DbConnectionService dbConnectionService;

    @Tool({
        "[GOAL] Start source resolution by discovering which connections the user can operate on.",
        "[PRECHECK] Call this before assuming any connection when current context is missing or ambiguous.",
        "[WHEN] Use when connectionId is unknown, multiple data sources may match, or user asks to switch source.",
        "[AFTER] Use result to narrow candidate sources, then continue with getCatalogNames/searchObjects."
    })
    public AgentToolResult getConnections(InvocationParameters parameters) {
        log.info("{} getConnections", "[Tool]");
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return AgentToolResult.noContext();
            }
            List<ConnectionResponse> connections = dbConnectionService.getAllConnections(userId);
            if (connections == null || connections.isEmpty()) {
                log.info("{} getConnections -> empty", "[Tool done]");
                return AgentToolResult.empty();
            }
            log.info("{} getConnections, result size={}", "[Tool done]", connections.size());
            return AgentToolResult.success(AgentConnectionView.fromList(connections));
        } catch (Exception e) {
            log.error("{} getConnections", "[Tool error]", e);
            return AgentToolResult.fail(e);
        }
    }

    @Tool({
        "[GOAL] Inspect one selected connection in detail to validate target source context.",
        "[PRECHECK] connectionId should come from session context or getConnections.",
        "[WHEN] Use when host/port/default-database details are needed for planning or disambiguation.",
        "[AFTER] If multiple candidates remain, askUserQuestion before proceeding to SQL."
    })
    public AgentToolResult getConnectionById(
            @P("The connection id (from session context or getConnections result)") Long connectionId,
            InvocationParameters parameters) {
        log.info("[Tool] getConnectionById, connectionId={}", connectionId);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return AgentToolResult.noContext();
            }
            ConnectionResponse connection = dbConnectionService.getConnectionById(connectionId, userId);
            log.info("[Tool done] getConnectionById, connectionId={}", connectionId);
            return AgentToolResult.success(AgentConnectionView.from(connection));
        } catch (Exception e) {
            log.error("[Tool error] getConnectionById, connectionId={}", connectionId, e);
            return AgentToolResult.fail(e);
        }
    }
}
