package edu.zsc.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionResponse;
import edu.zsc.ai.domain.service.db.DbConnectionService;
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
        "List all database connections owned by the current user.",
        "Use when the user asks for their connections, wants to switch connection, or needs to see available connections."
    })
    public String getMyConnections(InvocationParameters parameters) {
        log.info("[Tool before] getMyConnections");
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return "User context missing.";
            }
            List<ConnectionResponse> connections = dbConnectionService.getAllConnections(userId);
            if (connections == null || connections.isEmpty()) {
                log.info("[Tool done] getMyConnections -> EMPTY: No connections found.");
                return "EMPTY: No connections found.";
            }
            log.info("[Tool done] getMyConnections, result size={}", connections.size());
            return edu.zsc.ai.util.JsonUtil.object2json(connections);
        } catch (Exception e) {
            log.error("[Tool error] getMyConnections", e);
            return e.getMessage();
        }
    }

    @Tool({
        "Get details of a specific database connection by its connectionId.",
        "Use when you need full connection info (host, port, database name, etc.) for a given connection id."
    })
    public String getConnectionById(
            @P("The connection id (from session context or getMyConnections result)") Long connectionId,
            InvocationParameters parameters) {
        log.info("[Tool before] getConnectionById, connectionId={}", connectionId);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return "User context missing.";
            }
            ConnectionResponse connection = dbConnectionService.getConnectionById(connectionId, userId);
            log.info("[Tool done] getConnectionById, connectionId={}", connectionId);
            return edu.zsc.ai.util.JsonUtil.object2json(connection);
        } catch (Exception e) {
            log.error("[Tool error] getConnectionById, connectionId={}", connectionId, e);
            return e.getMessage();
        }
    }
}
