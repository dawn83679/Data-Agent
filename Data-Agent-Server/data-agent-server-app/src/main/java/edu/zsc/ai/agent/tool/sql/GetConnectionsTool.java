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
            "Value: returns the verified list of data-source connections currently available to the requesting user.",
            "Use When: the connection scope is missing, or the user asks which connections are available.",
            "Preconditions: none.",
            "Result: each item contains id, name, and dbType only.",
            "Boundary: do not invent hosts, ports, credentials, or hidden metadata."
    })
    public AgentToolResult getConnections(
            @P(value = ToolDescriptionParam.UI_STEP_DESCRIPTION, required = false) String description,
            InvocationParameters parameters) {
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

    public AgentToolResult getConnections(InvocationParameters parameters) {
        return getConnections(null, parameters);
    }

    private AvailableConnectionItem mapToAvailableConnectionItem(ConnectionResponse connection) {
        return new AvailableConnectionItem(connection.getId(), connection.getName(), connection.getDbType());
    }
}
