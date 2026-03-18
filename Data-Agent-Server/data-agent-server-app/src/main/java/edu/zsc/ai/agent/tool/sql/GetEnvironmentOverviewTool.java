package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.sql.model.ConnectionOverview;
import edu.zsc.ai.domain.service.db.DiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Returns the full connection/catalog/schema landscape.
 * MainAgent uses this to list connections before delegating to Explorer.
 */
@AgentTool
@Slf4j
@Component
@RequiredArgsConstructor
public class GetEnvironmentOverviewTool {

    private final DiscoveryService discoveryService;

    @Tool({
            "Returns the complete environment overview: all connections, catalogs, and schemas in one shot.",
            "Use at the start when the environment is unknown, or to list available connections for the user.",
            "For PostgreSQL, schemas (excluding system) are included. For MySQL, schemas are empty arrays.",
            "Response includes elapsedMs — if > 2000ms, narrow scope in later calls."
    })
    public AgentToolResult getEnvironmentOverview(InvocationParameters parameters) {
        log.info("[Tool] getEnvironmentOverview");
        List<ConnectionOverview> overview = discoveryService.getEnvironmentOverview();
        if (CollectionUtils.isEmpty(overview)) {
            log.info("[Tool done] getEnvironmentOverview -> empty");
            return AgentToolResult.empty();
        }
        log.info("[Tool done] getEnvironmentOverview, connections={}", overview.size());
        return AgentToolResult.builder()
                .success(true)
                .message(buildOverviewMessage(overview))
                .result(overview)
                .build();
    }

    private String buildOverviewMessage(List<ConnectionOverview> overview) {
        List<ConnectionOverview> unavailableConnections = overview.stream()
                .filter(connection -> StringUtils.isNotBlank(connection.error()))
                .toList();
        if (CollectionUtils.isEmpty(unavailableConnections)) {
            return "All listed connections are currently available.";
        }

        String unavailableSummary = unavailableConnections.stream()
                .map(connection -> String.format("%s(id=%s)",
                        connection.name(),
                        connection.id()))
                .collect(Collectors.joining("; "));

        long availableCount = overview.size() - unavailableConnections.size();
        if (availableCount > 0) {
            return "Unavailable connections found: "
                    + unavailableSummary
                    + ". Ask the user whether to switch to an available connection or retry later. Do not continue object discovery until the user replies.";
        }

        return "No available connections: "
                + unavailableSummary
                + ". Ask the user whether to retry later or check the connection configuration. Do not continue object discovery until the user replies.";
    }
}
