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
import java.util.Objects;

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
<<<<<<< HEAD
            "Scope defaults: if current context already provides connection, database, or schema, searching within that scope is often the cheapest next step.",
            "Pattern syntax: use SQL wildcards such as %order% or %user_% for objectNamePattern, databaseNamePattern, and schemaNamePattern.",
=======
            "Use When: useful when an approximate object name, keyword, or naming pattern can help discovery. If current context already provides connection, database, or schema, searching within that scope is often the cheapest next step. Use SQL wildcards such as %order% or %user_% for objectNamePattern, databaseNamePattern, and schemaNamePattern.",
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
            "Preconditions: objectNamePattern is required. databaseNamePattern requires connectionId. schemaNamePattern requires connectionId plus databaseNamePattern.",
            "After Success: the returned matches can support candidate comparison, deeper inspection with getObjectDetail, focused questioning, or broader discovery.",
            "After Partial Success: some scopes may return useful matches while others remain incomplete.",
            "After Failure: refine the pattern, adjust the scope, or gather more context before trying again.",
<<<<<<< HEAD
            "Result limits: results are capped at 100. If objectType is omitted, TABLE and VIEW are searched."
=======
            "Relation: often helpful before getObjectDetail or callingExplorerSubAgent. Results are capped at 100. If objectType is omitted, TABLE and VIEW are searched."
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
    })
    public AgentToolResult searchObjects(
            @P("Search query parameters") ObjectSearchQuery query,
            InvocationParameters parameters) {
        String objectNamePattern = query.getObjectNamePattern();
        String objectType = query.getObjectType();
        boolean explorerScope = AgentRequestContext.isExplorerScope();
        Long requestConnectionId = RequestContext.getConnectionId();
        Long connectionId = ConnectionIdUtil.toLong(query.getConnectionId());
        String databaseNamePattern = query.getDatabaseNamePattern();
        String schemaNamePattern = query.getSchemaNamePattern();
        if (connectionId == null) {
            boolean useDefaultConnection = !explorerScope
                    || StringUtils.isNotBlank(databaseNamePattern)
                    || StringUtils.isNotBlank(schemaNamePattern);
            if (useDefaultConnection) {
                connectionId = requestConnectionId;
            }
        }
        if (Objects.equals(connectionId, requestConnectionId)) {
            if (StringUtils.isBlank(databaseNamePattern)) {
                databaseNamePattern = RequestContext.getCatalog();
            }
            if (StringUtils.isBlank(schemaNamePattern)) {
                schemaNamePattern = RequestContext.getSchema();
            }
        }

        if (explorerScope) {
            if (connectionId != null) {
                ExplorerConnectionScopeGuard.validateConnectionAllowed(connectionId);
            } else {
                AgentRequestContext.requireAllowedConnectionIds();
            }
        }

        log.info("[Tool] searchObjects, pattern={}, type={}, connectionId={}, databasePattern={}, schemaPattern={}",
                objectNamePattern, objectType, connectionId, databaseNamePattern, schemaNamePattern);

        if (StringUtils.isNotBlank(schemaNamePattern) && StringUtils.isBlank(databaseNamePattern)) {
            throw AgentToolExecuteException.invalidInput(
                    ToolNameEnum.SEARCH_OBJECTS,
                    "schemaNamePattern requires databaseNamePattern to be specified. Add databaseNamePattern before retrying searchObjects. "
                            + "Do not continue object discovery until the scope is valid."
            );
        }
        if (StringUtils.isNotBlank(databaseNamePattern) && connectionId == null) {
            throw AgentToolExecuteException.invalidInput(
                    ToolNameEnum.SEARCH_OBJECTS,
                    "databaseNamePattern requires connectionId to be specified. Add connectionId before retrying searchObjects. "
                            + "Do not continue object discovery until the scope is valid."
            );
        }

        DatabaseObjectTypeEnum normalizedType = StringUtils.isNotBlank(objectType)
                ? DatabaseObjectTypeEnum.parseQueryable(objectType)
                : null;

        ObjectSearchResponse response = discoveryService.searchObjects(
                objectNamePattern, normalizedType, connectionId, databaseNamePattern, schemaNamePattern);

        if (CollectionUtils.isNotEmpty(response.errors())) {
            log.info("[Tool done] searchObjects, resultCount={}, truncated={}, errorCount={}",
                    response.totalCount(), response.truncated(), response.errors().size());
            return AgentToolResult.builder()
                    .success(true)
                    .message(buildSearchMessage(response, objectNamePattern, connectionId, databaseNamePattern, schemaNamePattern))
                    .result(response)
                    .build();
        }

        if (CollectionUtils.isEmpty(response.results())) {
            log.info("[Tool done] searchObjects -> empty");
            return AgentToolResult.empty(buildEmptySearchMessage(objectNamePattern, connectionId, databaseNamePattern, schemaNamePattern));
        }

        log.info("[Tool done] searchObjects, resultCount={}, truncated={}",
                response.totalCount(), response.truncated());
        return AgentToolResult.success(response, buildSearchSuccessMessage(response, objectNamePattern, connectionId, databaseNamePattern, schemaNamePattern));
    }

    private String buildSearchMessage(ObjectSearchResponse response,
                                      String objectNamePattern,
                                      Long connectionId,
                                      String databaseNamePattern,
                                      String schemaNamePattern) {
        String errorSummary = String.join("; ", response.errors());
        String scope = buildScopeLabel(objectNamePattern, connectionId, databaseNamePattern, schemaNamePattern);
        String baseMessage = "Object search encountered scope failures for " + scope + ". Scope failures: " + errorSummary + ".";
        if (CollectionUtils.isNotEmpty(response.results())) {
            return ToolMessageSupport.sentence(
                    baseMessage,
                    "Use askUserQuestion to ask the user to clarify the target scope before continuing."
            );
        }

        return ToolMessageSupport.sentence(
                baseMessage,
                "Use askUserQuestion to ask the user to clarify the target scope before continuing."
        );
    }

    private String buildSearchSuccessMessage(ObjectSearchResponse response,
                                             String objectNamePattern,
                                             Long connectionId,
                                             String databaseNamePattern,
                                             String schemaNamePattern) {
        String scope = buildScopeLabel(objectNamePattern, connectionId, databaseNamePattern, schemaNamePattern);
        String truncation = response.truncated()
                ? " The result set is truncated, so refine the search before assuming all matches are visible."
                : "";
        if (isFuzzyPattern(objectNamePattern)) {
            return ToolMessageSupport.sentence(
                    "Object search found " + response.totalCount() + " candidate(s) for " + scope + ".",
                    "Use askUserQuestion to ask the user to narrow the target object before continuing." + truncation
            );
        }
        return ToolMessageSupport.sentence(
                "Object search found " + response.totalCount() + " candidate(s) for " + scope + ".",
                "Use askUserQuestion to ask the user to narrow the target object before continuing." + truncation
        );
    }

    private String buildEmptySearchMessage(String objectNamePattern,
                                           Long connectionId,
                                           String databaseNamePattern,
                                           String schemaNamePattern) {
        return ToolMessageSupport.sentence(
                "Object search returned no matches for "
                        + buildScopeLabel(objectNamePattern, connectionId, databaseNamePattern, schemaNamePattern) + ".",
                "Use askUserQuestion to ask the user to clarify the target before continuing."
        );
    }

    private String buildScopeLabel(String objectNamePattern,
                                   Long connectionId,
                                   String databaseNamePattern,
                                   String schemaNamePattern) {
        StringBuilder builder = new StringBuilder();
        builder.append("pattern=")
                .append(StringUtils.defaultIfBlank(objectNamePattern, "<blank>"));
        if (connectionId != null) {
            builder.append(", connectionId=").append(connectionId);
        }
        if (StringUtils.isNotBlank(databaseNamePattern)) {
            builder.append(", databasePattern=").append(databaseNamePattern);
        }
        if (StringUtils.isNotBlank(schemaNamePattern)) {
            builder.append(", schemaPattern=").append(schemaNamePattern);
        }
        return builder.toString();
    }

    private boolean isFuzzyPattern(String objectNamePattern) {
        return StringUtils.contains(objectNamePattern, '%') || StringUtils.contains(objectNamePattern, '_');
    }
}
