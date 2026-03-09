package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.sql.model.ConnectionOverview;
import edu.zsc.ai.agent.tool.sql.model.NamedObjectDetail;
import edu.zsc.ai.agent.tool.sql.model.ObjectQueryItem;
import edu.zsc.ai.agent.tool.sql.model.ObjectSearchQuery;
import edu.zsc.ai.agent.tool.sql.model.ObjectSearchResponse;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.domain.service.db.DiscoveryService;
import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

@AgentTool
@Slf4j
@RequiredArgsConstructor
public class DiscoveryTool {

    private final DiscoveryService discoveryService;

    @Tool({
            "Returns the complete environment overview: all connections with their databases (catalogs) ",
            "and schemas in a single nested structure. This replaces the need to call getConnections and ",
            "getCatalogNames separately — one call gives you the full landscape.",
            "",
            "Call this at the start of every new request to understand the user's data environment. ",
            "For PostgreSQL, schemas (excluding system schemas) are included under each catalog. ",
            "For MySQL, schemas will be empty arrays since MySQL has no schema layer.",
            "",
            "Response includes elapsedMs — if > 2000ms, the user has many connections or slow networks; ",
            "keep this in mind when choosing how broadly to search with subsequent calls."
    })
    public AgentToolResult getEnvironmentOverview(InvocationParameters parameters) {
        return AgentToolResult.timed(() -> {
            log.info("[Tool] getEnvironmentOverview");
            try {
                Long userId = parameters.get(RequestContextConstant.USER_ID);
                if (Objects.isNull(userId)) {
                    return AgentToolResult.noContext();
                }
                List<ConnectionOverview> overview = discoveryService.getEnvironmentOverview(userId);
                if (CollectionUtils.isEmpty(overview)) {
                    log.info("[Tool done] getEnvironmentOverview -> empty");
                    return AgentToolResult.empty();
                }
                log.info("[Tool done] getEnvironmentOverview, connections={}", overview.size());
                return AgentToolResult.success(overview);
            } catch (Exception e) {
                log.error("[Tool error] getEnvironmentOverview", e);
                String errorMsg = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
                return AgentToolResult.fail("Failed to get environment overview: " + errorMsg);
            }
        });
    }

    @Tool({
            "Global search for database objects (tables, views, functions, procedures, triggers) across ",
            "all connections and databases. Returns a flat list with full location path for each match.",
            "",
            "Use SQL wildcards in the pattern: '%order%' finds any object containing 'order', ",
            "'user_%' finds objects starting with 'user_'. Optional filters narrow the search scope: ",
            "connectionId → specific connection, databaseName → specific database (requires connectionId), ",
            "schemaName → specific schema (requires connectionId + databaseName). ",
            "Results are capped at 100. If not specifying objectType, searches TABLE + VIEW by default.",
            "",
            "Response includes elapsedMs. If a call took > 2000ms, narrow scope next time by passing ",
            "connectionId/databaseName from previous results to avoid repeating the cost."
    })
    public AgentToolResult searchObjects(
            @P("Search query parameters") ObjectSearchQuery query,
            InvocationParameters parameters) {
        return AgentToolResult.timed(() -> {
            String objectNamePattern = query.getObjectNamePattern();
            String objectType = query.getObjectType();
            Long connectionId = query.getConnectionId();
            String databaseName = query.getDatabaseName();
            String schemaName = query.getSchemaName();

            log.info("[Tool] searchObjects, pattern={}, type={}, connectionId={}, database={}, schema={}",
                    objectNamePattern, objectType, connectionId, databaseName, schemaName);
            try {
                Long userId = parameters.get(RequestContextConstant.USER_ID);
                if (Objects.isNull(userId)) {
                    return AgentToolResult.noContext();
                }

                // Validate parameter dependency chain
                if (StringUtils.isNotBlank(schemaName) && StringUtils.isBlank(databaseName)) {
                    return AgentToolResult.fail("schemaName requires databaseName to be specified.");
                }
                if (StringUtils.isNotBlank(databaseName) && Objects.isNull(connectionId)) {
                    return AgentToolResult.fail("databaseName requires connectionId to be specified.");
                }

                DatabaseObjectTypeEnum normalizedType = StringUtils.isNotBlank(objectType)
                        ? DatabaseObjectTypeEnum.parseQueryable(objectType)
                        : null;

                ObjectSearchResponse response = discoveryService.searchObjects(
                        objectNamePattern, normalizedType, connectionId, databaseName, schemaName, userId);

                if (CollectionUtils.isEmpty(response.results())) {
                    log.info("[Tool done] searchObjects -> empty");
                    return AgentToolResult.empty();
                }

                log.info("[Tool done] searchObjects, resultCount={}, truncated={}",
                        response.totalCount(), response.truncated());
                return AgentToolResult.success(response);
            } catch (Exception e) {
                log.error("[Tool error] searchObjects, pattern={}", objectNamePattern, e);
                String errorMsg = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
                return AgentToolResult.fail("Failed to search objects with pattern '" + objectNamePattern + "': " + errorMsg);
            }
        });
    }

    @Tool({
            "Returns complete details for one or more database objects in a single call: DDL (structure), ",
            "row count, and index information. Accepts a list of objects — pass multiple objects to ",
            "retrieve all their details at once and save LLM reasoning rounds.",
            "",
            "For TABLE: returns DDL + rowCount + indexes. For VIEW: returns DDL + rowCount (no indexes). ",
            "For FUNCTION/PROCEDURE/TRIGGER: returns DDL only. Call this for EVERY table you plan to ",
            "reference in SQL — the DDL is your ground truth for column names, types, and constraints.",
            "",
            "Each object in the result includes success/error fields — a single object's failure does ",
            "not affect the others. Response includes elapsedMs."
    })
    public AgentToolResult getObjectDetail(
            @P("List of objects to retrieve details for") List<ObjectQueryItem> objects,
            InvocationParameters parameters) {
        return AgentToolResult.timed(() -> {
            log.info("[Tool] getObjectDetail, objectCount={}", CollectionUtils.size(objects));
            try {
                Long userId = parameters.get(RequestContextConstant.USER_ID);
                if (Objects.isNull(userId)) {
                    return AgentToolResult.noContext();
                }
                if (CollectionUtils.isEmpty(objects)) {
                    return AgentToolResult.fail("objects list must not be empty.");
                }

                List<NamedObjectDetail> results = discoveryService.getObjectDetails(objects, userId);

                log.info("[Tool done] getObjectDetail, requested={}, succeeded={}",
                        objects.size(), results.stream().filter(NamedObjectDetail::success).count());
                return AgentToolResult.success(results);
            } catch (Exception e) {
                log.error("[Tool error] getObjectDetail", e);
                String errorMsg = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
                return AgentToolResult.fail("Failed to get object details: " + errorMsg);
            }
        });
    }
}
