package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ToolContext;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.sql.model.ConnectionOverview;
import edu.zsc.ai.agent.tool.sql.model.NamedObjectDetail;
import edu.zsc.ai.agent.tool.sql.model.ObjectQueryItem;
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
            "Calling this tool greatly improves target accuracy — you get the full connection/catalog/schema ",
            "landscape in one shot and avoid wrong-database or wrong-table operations. ",
            "Returns the complete environment overview in a single nested structure.",
            "",
            "When to Use: at the start of a new request when the environment is unknown.",
            "When NOT to Use: when you already have full connection/catalog/schema context from this conversation.",
            "Relation: call first before searchObjects when scanning; then use searchObjects to find objects, getObjectDetail for structure.",
            "",
            "For PostgreSQL, schemas (excluding system schemas) are included under each catalog. ",
            "For MySQL, schemas will be empty arrays. Response includes elapsedMs — if > 2000ms, narrow scope in later calls."
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
            "Using this tool significantly improves discovery — it does pattern (fuzzy) search across all connections. ",
            "You do NOT need the exact object name: pass a pattern with SQL wildcards (e.g. %order% finds any name containing 'order', %user_% finds names starting with 'user_'). ",
            "When the user mentions a keyword like '订单' or 'order' or 'user table', call this with objectNamePattern like '%order%' or '%user%' to find all matching tables/views, then getObjectDetail or askUserQuestion as needed.",
            "",
            "When to Use: when you have only a keyword or partial name from the user — use pattern search (e.g. %keyword%); after getEnvironmentOverview when narrowing scope.",
            "When NOT to Use: only when you already have exact object identity from a previous result (e.g. user chose one from a list) — then use getObjectDetail directly with that list.",
            "Relation: use after getEnvironmentOverview when env unknown; use before getObjectDetail to get candidates. Always use wildcards for discovery; exact names only when re-querying a known object.",
            "",
            "Required: only objectNamePattern (use % and _, e.g. '%order%', 'user_%'). All others optional: connectionId, databaseName, schemaName (omit to search all connections/databases/schemas). If narrowing: databaseName requires connectionId; schemaName requires connectionId + databaseName. Results capped at 100. objectType omitted = TABLE + VIEW. Response includes elapsedMs."
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

    @Tool({
            "Calling this tool greatly improves SQL correctness — you get real DDL, row counts, and indexes ",
            "instead of guessing column names or types; pass multiple objects in one call to save rounds. ",
            "Returns complete details: DDL (structure), row count, and index information per object.",
            "",
            "When to Use: before writing any SQL that references tables; after searchObjects or when target is confirmed.",
            "When NOT to Use: when you only need object names (searchObjects is enough); do not call repeatedly for the same object in one turn.",
            "Relation: after getEnvironmentOverview/searchObjects for targets; pass all tables involved in a JOIN in one getObjectDetail call.",
            "",
            "TABLE: DDL + rowCount + indexes. VIEW: DDL + rowCount. FUNCTION/PROCEDURE/TRIGGER: DDL only. Each object has success/error; one failure does not affect others. Response includes elapsedMs."
    })
    public AgentToolResult getObjectDetail(
            @P("List of objects to retrieve details for") List<ObjectQueryItem> objects,
            InvocationParameters parameters) {
        try (var ctx = ToolContext.from(parameters)) {
            log.info("[Tool] getObjectDetail, objectCount={}", CollectionUtils.size(objects));
            if (CollectionUtils.isEmpty(objects)) {
                return AgentToolResult.fail("objects list must not be empty.");
            }

            List<NamedObjectDetail> results = discoveryService.getObjectDetails(objects);

            log.info("[Tool done] getObjectDetail, requested={}, succeeded={}",
                    objects.size(), results.stream().filter(NamedObjectDetail::success).count());
            return ctx.timed(AgentToolResult.success(results));
        } catch (Exception e) {
            log.error("[Tool error] getObjectDetail", e);
            String errorMsg = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
            return AgentToolResult.fail("Failed to get object details: " + errorMsg);
        }
    }
}
