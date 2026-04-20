package edu.zsc.ai.agent.tool.sql;

import java.util.List;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
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
            "Value: returns the verified list of data-source connections currently available to the requesting user.",
            "Preconditions: none.",
            "Result shape: each returned item includes only id, name, and dbType.",
            "After Success: use the returned id, name, and dbType to answer connection-inventory questions or choose the next scope-discovery step.",
            "After Failure: tell the user that the connection inventory could not be loaded. Do not invent hosts, ports, or hidden metadata."
    })
    public AgentToolResult getConnections(InvocationParameters parameters) {
        log.info("[Tool] getConnections");
        try {
            List<AvailableConnectionItem> connections = dbConnectionService.getAllConnections().stream()
                    .map(this::mapToAvailableConnectionItem)
                    .toList();
            if (connections.isEmpty()) {
                log.info("[Tool done] getConnections -> empty");
                return AgentToolResult.empty("No available connections were found for the current session.");
            }
            log.info("[Tool done] getConnections count={}", connections.size());
            return AgentToolResult.success(connections,
                    ToolMessageSupport.sentence(
                            "Returned " + connections.size() + " available connection(s) for the current session.",
                            "If the current task still lacks a grounded connection scope, use askUserQuestion to ask the user which connection should be used before continuing."
                    ));
        } catch (Exception e) {
            log.warn("[Tool] getConnections failed: {}", e.getMessage());
            return AgentToolResult.fail(
                    "Failed to list available connections: " + e.getMessage());
        }
    }

    private AvailableConnectionItem mapToAvailableConnectionItem(ConnectionResponse connection) {
        return new AvailableConnectionItem(connection.getId(), connection.getName(), connection.getDbType());
    }
}
