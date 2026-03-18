package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
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
            "Use When: call at the start when the environment is unknown or when the user needs to choose a target connection.",
            "After Success: compare the returned connections, narrow to the most likely target, and askUserQuestion if multiple connections remain plausible.",
            "After Partial Success: continue only with connections that returned metadata; do not assume failed connections are usable.",
            "After Failure: do not continue discovery, planning, or execution until a usable connection is available.",
            "Relation: usually before searchObjects or callingExplorerSubAgent; use the returned connectionId as downstream scope.",
            "PostgreSQL includes non-system schemas. MySQL returns empty schema arrays. If elapsedMs is high, narrow scope in later discovery."
    })
    public AgentToolResult getEnvironmentOverview(InvocationParameters parameters) {
        log.info("[Tool] getEnvironmentOverview");
        List<ConnectionOverview> overview = discoveryService.getEnvironmentOverview();
        if (CollectionUtils.isEmpty(overview)) {
            log.info("[Tool done] getEnvironmentOverview -> empty");
            return AgentToolResult.empty(ToolMessageSupport.sentence(
                    "Environment overview returned no available connections.",
                    ToolMessageSupport.askUserWhether("verify the connection configuration or retry later"),
                    ToolMessageSupport.DO_NOT_PROCEED_WITH_DISCOVERY_PLANNING_OR_EXECUTION_UNTIL_CONNECTION_IS_AVAILABLE
            ));
        }
        log.info("[Tool done] getEnvironmentOverview, connections={}", overview.size());
        return AgentToolResult.success(overview, buildOverviewMessage(overview));
    }

    private String buildOverviewMessage(List<ConnectionOverview> overview) {
        List<ConnectionOverview> unavailableConnections = overview.stream()
                .filter(connection -> StringUtils.isNotBlank(connection.error()))
                .toList();
        if (CollectionUtils.isEmpty(unavailableConnections)) {
            return ToolMessageSupport.sentence(
                    "Environment overview is available for " + overview.size() + " connection(s).",
                    "Compare candidate objects across available connections before choosing one.",
                    "If multiple connections remain plausible, ask the user to confirm the target connection before proceeding to discovery, planning, or execution."
            );
        }

        String unavailableSummary = unavailableConnections.stream()
                .map(connection -> String.format("%s(id=%s): %s",
                        connection.name(),
                        connection.id(),
                        connection.error()))
                .collect(Collectors.joining("; "));

        long availableCount = overview.size() - unavailableConnections.size();
        if (availableCount > 0) {
            return ToolMessageSupport.sentence(
                    "Environment overview is only partially available. Failed connections: " + unavailableSummary + ".",
                    ToolMessageSupport.continueOnlyWith("the remaining available connections"),
                    ToolMessageSupport.askUserWhether("switch to an available connection or retry later"),
                    ToolMessageSupport.DO_NOT_CONTINUE_OBJECT_DISCOVERY_UNTIL_USER_REPLIES
            );
        }

        return ToolMessageSupport.sentence(
                "Environment overview could not find any usable connection. Failed connections: " + unavailableSummary + ".",
                ToolMessageSupport.askUserWhether("retry later or check the connection configuration"),
                ToolMessageSupport.DO_NOT_CONTINUE_OBJECT_DISCOVERY_UNTIL_USER_REPLIES
        );
    }
}
