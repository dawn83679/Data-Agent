package edu.zsc.ai.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionResponse;
import edu.zsc.ai.domain.service.db.DbConnectionService;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ConnectionTool {

    private final DbConnectionService dbConnectionService;

    @Tool({
        "[WHAT] List all database connections owned by the current user.",
        "[WHEN] Use when connectionId is not known, the user asks about their connections, or wants to switch to a different connection."
    })
    public AgentToolResult getMyConnections(InvocationParameters parameters) {
        log.info("{} getMyConnections", "[Tool]");
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return AgentToolResult.noContext();
            }
            List<ConnectionResponse> connections = dbConnectionService.getAllConnections(userId);
            if (connections == null || connections.isEmpty()) {
                log.info("{} getMyConnections -> empty", "[Tool done]");
                return AgentToolResult.empty();
            }
            log.info("{} getMyConnections, result size={}", "[Tool done]", connections.size());
            return AgentToolResult.success(connections);
        } catch (Exception e) {
            log.error("{} getMyConnections", "[Tool error]", e);
            return AgentToolResult.fail(e);
        }
    }

    @Tool({
        "[WHAT] Get full details of a specific database connection by its ID.",
        "[WHEN] Use when you need host, port, or database name for a given connectionId from session context or getMyConnections."
    })
    public AgentToolResult getConnectionById(
            @P("The connection id (from session context or getMyConnections result)") Long connectionId,
            InvocationParameters parameters) {
        log.info("[Tool] getConnectionById, connectionId={}", connectionId);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return AgentToolResult.noContext();
            }
            ConnectionResponse connection = dbConnectionService.getConnectionById(connectionId, userId);
            log.info("[Tool done] getConnectionById, connectionId={}", connectionId);
            return AgentToolResult.success(connection);
        } catch (Exception e) {
            log.error("[Tool error] getConnectionById, connectionId={}", connectionId, e);
            return AgentToolResult.fail(e);
        }
    }
}
