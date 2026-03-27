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
            "Value: maps the available connections, catalogs, and schemas in one call so you can choose a valid discovery scope.",
            "Use When: useful when you want a broad picture of the available connections and catalogs, especially before narrowing scope or preparing a clarification question.",
            "After Success: the returned overview can support follow-up questions, cross-connection comparison, or focused discovery in a narrower scope.",
            "After Partial Success: some connections may be usable while others still need attention; interpret the overview accordingly.",
            "After Failure: the environment information is not currently available, so the next step may be clarification, retry, or stopping with an explicit limitation.",
            "Relation: often helpful before clarification or discovery, but not automatically required for every database request.",
            "PostgreSQL includes non-system schemas. MySQL returns empty schema arrays. If elapsedMs is high, narrow scope in later discovery."
    })
    public AgentToolResult getEnvironmentOverview(InvocationParameters parameters) {
        log.info("[Tool] getEnvironmentOverview");
        List<ConnectionOverview> overview = discoveryService.getEnvironmentOverview();
        if (CollectionUtils.isEmpty(overview)) {
            log.info("[Tool done] getEnvironmentOverview -> empty");
            return AgentToolResult.empty(
                    "Environment overview returned no usable connections. Use askUserQuestion to ask the user whether to retry later or check the connection configuration.");
        }
        log.info("[Tool done] getEnvironmentOverview, connections={}", overview.size());
        return AgentToolResult.success(overview, buildOverviewMessage(overview));
    }

    private String buildOverviewMessage(List<ConnectionOverview> overview) {
        List<ConnectionOverview> unavailableConnections = overview.stream()
                .filter(connection -> StringUtils.isNotBlank(connection.error()))
                .toList();
        long availableCount = overview.size() - unavailableConnections.size();
        if (availableCount > 0) {
            String failedSummary = CollectionUtils.isEmpty(unavailableConnections)
                    ? ""
                    : " Failed connections: " + unavailableConnections.stream()
                    .map(connection -> String.format("%s(id=%s): %s",
                            connection.name(),
                            connection.id(),
                            connection.error()))
                    .collect(Collectors.joining("; ")) + ".";
            return "Environment overview found " + availableCount
                    + " usable connection(s)." + failedSummary
                    + " Use askUserQuestion to ask the user to narrow the search scope before continuing.";
        }

        return "Environment overview returned no usable connections. Use askUserQuestion to ask the user whether to retry later or check the connection configuration.";
    }
}
