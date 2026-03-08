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
        "The foundation of every task — calling this first dramatically reduces the chance ",
        "of querying the wrong data source. Returns all database connections with connectionId, ",
        "name, and type, giving you a complete map of the user's data environment.",
        "",
        "Without this, you're guessing which connection to use — the #1 cause of wrong results. ",
        "Call this at the start of every new request to enable breadth-first discovery. ",
        "Seeing all connections prevents tunnel vision on a single source."
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
