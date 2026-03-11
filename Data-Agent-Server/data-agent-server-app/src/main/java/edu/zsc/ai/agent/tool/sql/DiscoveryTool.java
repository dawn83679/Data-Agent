package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ToolContext;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.sql.model.ConnectionOverview;
import edu.zsc.ai.agent.tool.sql.model.ObjectSearchQuery;
import edu.zsc.ai.agent.tool.sql.model.ObjectSearchResponse;
import edu.zsc.ai.domain.service.db.DiscoveryService;
import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@AgentTool
@Slf4j
@RequiredArgsConstructor
public class DiscoveryTool {

    private final DiscoveryService discoveryService;

    @Tool({
            "Returns the complete environment overview: all connections with their databases and schemas ",
            "in a single nested structure.",
            "",
            "Use when: target data source is unclear, need to discover available connections/databases.",
            "Skip when: connectionId and database are already confirmed in this conversation.",
            "",
            "Response includes elapsedMs. If >2000ms, narrow scope in subsequent calls."
    })
    public AgentToolResult getEnvironmentOverview(InvocationParameters parameters) {
        try (var ctx = ToolContext.from(parameters)) {
            log.info("[Tool] getEnvironmentOverview");
            List<ConnectionOverview> overview = discoveryService.getEnvironmentOverview();
            if (CollectionUtils.isEmpty(overview)) {
                log.info("[Tool done] getEnvironmentOverview -> empty");
                return ctx.timed(AgentToolResult.empty());
            }
            log.info("[Tool done] getEnvironmentOverview, connections={}", overview.size());
            return ctx.timed(AgentToolResult.success(overview));
        } catch (Exception e) {
            log.error("[Tool error] getEnvironmentOverview", e);
            String errorMsg = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
            return AgentToolResult.fail("Failed to get environment overview: " + errorMsg);
        }
    }

    @Tool({
            "Searches database objects (tables, views, functions, procedures, triggers) across all connections. ",
            "Returns flat list with full location path per match, capped at 100.",
            "",
            "Use SQL wildcards: '%order%' for fuzzy match, 'user_%' for prefix match.",
            "Optional filters: connectionId, databaseName, schemaName to narrow scope.",
            "Default objectType: TABLE + VIEW.",
            "",
            "Use when: finding candidate objects by name pattern.",
            "Skip when: exact object names and locations already known.",
            "",
            "Response includes elapsedMs. If >2000ms, pass connectionId/databaseName next time."
    })
    public AgentToolResult searchObjects(
            @P("Search query parameters") ObjectSearchQuery query,
            InvocationParameters parameters) {
    try (var ctx = ToolContext.from(parameters)) {
            String objectNamePattern = query.getObjectNamePattern();
            String objectType = query.getObjectType();
            Long connectionId = query.getConnectionId();
            String databaseName = query.getDatabaseName();
            String schemaName = query.getSchemaName();

            log.info("[Tool] searchObjects, pattern={}, type={}, connectionId={}, database={}, schema={}",
                    objectNamePattern, objectType, connectionId, databaseName, schemaName);

            if (StringUtils.isNotBlank(schemaName) && StringUtils.isBlank(databaseName)) {
                return AgentToolResult.fail("schemaName requires databaseName to be specified.");
            }
            if (StringUtils.isNotBlank(databaseName) && connectionId == null) {
                return AgentToolResult.fail("databaseName requires connectionId to be specified.");
            }

            DatabaseObjectTypeEnum normalizedType = StringUtils.isNotBlank(objectType)
                    ? DatabaseObjectTypeEnum.parseQueryable(objectType)
                    : null;

            ObjectSearchResponse response = discoveryService.searchObjects(
                    objectNamePattern, normalizedType, connectionId, databaseName, schemaName);

            if (CollectionUtils.isEmpty(response.results())) {
                log.info("[Tool done] searchObjects -> empty");
                return ctx.timed(AgentToolResult.empty());
            }

            log.info("[Tool done] searchObjects, resultCount={}, truncated={}",
                    response.totalCount(), response.truncated());
            return ctx.timed(AgentToolResult.success(response));
        } catch (Exception e) {
            log.error("[Tool error] searchObjects, pattern={}", query.getObjectNamePattern(), e);
            String errorMsg = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
            return AgentToolResult.fail("Failed to search objects with pattern '" + query.getObjectNamePattern() + "': " + errorMsg);
        }
    }

}
