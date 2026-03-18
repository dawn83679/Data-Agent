package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.guard.ExplorerConnectionScopeGuard;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.util.ConnectionIdUtil;
import edu.zsc.ai.agent.tool.sql.model.ObjectSearchQuery;
import edu.zsc.ai.agent.tool.sql.model.ObjectSearchResponse;
import edu.zsc.ai.domain.service.db.DiscoveryService;
import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pattern (fuzzy) search across connections for tables/views/functions.
 * Explorer SubAgent uses this for schema discovery.
 */
@AgentTool
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchObjectsTool {

    private final DiscoveryService discoveryService;

    @Tool({
            "Value: narrows candidate tables, views, and other database objects by name pattern so later steps work from likely targets instead of guesses.",
            "Use When: call when you know an approximate object name but not its exact connection, database, or schema. Use SQL wildcards such as %order% or %user_%.",
            "Preconditions: objectNamePattern is required. databaseName requires connectionId. schemaName requires connectionId plus databaseName.",
            "After Success: use the returned candidates to pick the target object. If one candidate clearly matches, call getObjectDetail. If multiple remain plausible, askUserQuestion before planning or execution.",
            "After Partial Success: continue only with matches from successful scopes; do not assume failed scopes contain no matches.",
            "After Failure: refine the pattern or scope, or ask the user to clarify the target. Do not invent object existence.",
            "Relation: often after getEnvironmentOverview and before getObjectDetail or callingExplorerSubAgent. Results are capped at 100. If objectType is omitted, TABLE and VIEW are searched."
    })
    public AgentToolResult searchObjects(
            @P("Search query parameters") ObjectSearchQuery query,
            InvocationParameters parameters) {
        String objectNamePattern = query.getObjectNamePattern();
        String objectType = query.getObjectType();
        boolean explorerScope = AgentRequestContext.isExplorerScope();
        Long connectionId = ConnectionIdUtil.toLong(query.getConnectionId());
        String databaseName = query.getDatabaseName();
        String schemaName = query.getSchemaName();
        if (connectionId == null) {
            boolean useDefaultConnection = !explorerScope
                    || StringUtils.isNotBlank(databaseName)
                    || StringUtils.isNotBlank(schemaName);
            if (useDefaultConnection) {
                connectionId = RequestContext.getConnectionId();
            }
        }

        if (explorerScope) {
            if (connectionId != null) {
                ExplorerConnectionScopeGuard.validateConnectionAllowed(connectionId);
            } else {
                AgentRequestContext.requireAllowedConnectionIds();
            }
        }

        log.info("[Tool] searchObjects, pattern={}, type={}, connectionId={}, database={}, schema={}",
                objectNamePattern, objectType, connectionId, databaseName, schemaName);

        if (StringUtils.isNotBlank(schemaName) && StringUtils.isBlank(databaseName)) {
            throw AgentToolExecuteException.invalidInput(
                    ToolNameEnum.SEARCH_OBJECTS,
                    "schemaName requires databaseName to be specified. Add databaseName before retrying searchObjects. "
                            + "Do not continue object discovery until the scope is valid."
            );
        }
        if (StringUtils.isNotBlank(databaseName) && connectionId == null) {
            throw AgentToolExecuteException.invalidInput(
                    ToolNameEnum.SEARCH_OBJECTS,
                    "databaseName requires connectionId to be specified. Add connectionId before retrying searchObjects. "
                            + "Do not continue object discovery until the scope is valid."
            );
        }

        DatabaseObjectTypeEnum normalizedType = StringUtils.isNotBlank(objectType)
                ? DatabaseObjectTypeEnum.parseQueryable(objectType)
                : null;

        ObjectSearchResponse response = discoveryService.searchObjects(
                objectNamePattern, normalizedType, connectionId, databaseName, schemaName);

        if (CollectionUtils.isNotEmpty(response.errors())) {
            log.info("[Tool done] searchObjects, resultCount={}, truncated={}, errorCount={}",
                    response.totalCount(), response.truncated(), response.errors().size());
            return AgentToolResult.builder()
                    .success(true)
                    .message(buildSearchMessage(response, objectNamePattern, connectionId, databaseName, schemaName))
                    .result(response)
                    .build();
        }

        if (CollectionUtils.isEmpty(response.results())) {
            log.info("[Tool done] searchObjects -> empty");
            return AgentToolResult.empty(buildEmptySearchMessage(objectNamePattern, connectionId, databaseName, schemaName));
        }

        log.info("[Tool done] searchObjects, resultCount={}, truncated={}",
                response.totalCount(), response.truncated());
        return AgentToolResult.success(response, buildSearchSuccessMessage(response, objectNamePattern, connectionId, databaseName, schemaName));
    }

    private String buildSearchMessage(ObjectSearchResponse response,
                                      String objectNamePattern,
                                      Long connectionId,
                                      String databaseName,
                                      String schemaName) {
        String errorSummary = String.join("; ", response.errors());
        String scope = buildScopeLabel(objectNamePattern, connectionId, databaseName, schemaName);
        if (CollectionUtils.isNotEmpty(response.results())) {
            return ToolMessageSupport.sentence(
                    "Object search returned partial results for " + scope + ". Scope failures: " + errorSummary + ".",
                    ToolMessageSupport.continueOnlyWith("the currently returned matches"),
                    ToolMessageSupport.askUserWhether("keep these matches or adjust the connection or scope"),
                    ToolMessageSupport.DO_NOT_CONTINUE_OBJECT_DISCOVERY_UNTIL_USER_REPLIES
            );
        }

        return ToolMessageSupport.sentence(
                "Object search could not return reliable matches for " + scope + ". Scope failures: " + errorSummary + ".",
                ToolMessageSupport.askUserWhether("retry with another connection or scope"),
                ToolMessageSupport.DO_NOT_CONTINUE_OBJECT_DISCOVERY_UNTIL_USER_REPLIES
        );
    }

    private String buildSearchSuccessMessage(ObjectSearchResponse response,
                                             String objectNamePattern,
                                             Long connectionId,
                                             String databaseName,
                                             String schemaName) {
        String scope = buildScopeLabel(objectNamePattern, connectionId, databaseName, schemaName);
        String truncation = response.truncated()
                ? " The result set is truncated, so refine the search before assuming all matches are visible."
                : "";
        if (response.totalCount() == 1) {
            return ToolMessageSupport.sentence(
                    "Object search found 1 candidate for " + scope + ".",
                    "Use this match to request object details or continue planning only if it matches the user's intent."
                            + truncation
            );
        }
        return ToolMessageSupport.sentence(
                "Object search found " + response.totalCount() + " candidate(s) for " + scope + ".",
                "Use these matches to narrow down the target object.",
                "If multiple candidates remain plausible, ask the user to confirm the intended object before requesting details or planning SQL."
                        + truncation
        );
    }

    private String buildEmptySearchMessage(String objectNamePattern,
                                           Long connectionId,
                                           String databaseName,
                                           String schemaName) {
        return ToolMessageSupport.sentence(
                "Object search returned no matches for "
                        + buildScopeLabel(objectNamePattern, connectionId, databaseName, schemaName) + ".",
                "Do not assume the object exists.",
                "Adjust the pattern or scope, or ask the user to clarify the target before proceeding."
        );
    }

    private String buildScopeLabel(String objectNamePattern,
                                   Long connectionId,
                                   String databaseName,
                                   String schemaName) {
        StringBuilder builder = new StringBuilder();
        builder.append("pattern=")
                .append(StringUtils.defaultIfBlank(objectNamePattern, "<blank>"));
        if (connectionId != null) {
            builder.append(", connectionId=").append(connectionId);
        }
        if (StringUtils.isNotBlank(databaseName)) {
            builder.append(", database=").append(databaseName);
        }
        if (StringUtils.isNotBlank(schemaName)) {
            builder.append(", schema=").append(schemaName);
        }
        return builder.toString();
    }
}
