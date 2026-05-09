package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ToolDescriptionParam;
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
            "Value: finds candidate tables, views, and other objects by SQL wildcard pattern.",
            "Use When: object scope is not yet exact but connection/database/schema scope is narrow enough for lightweight discovery.",
            "Preconditions: objectNamePattern is required. databaseNamePattern requires connectionId. schemaNamePattern requires connectionId plus databaseNamePattern.",
            "Result: up to 100 matches; if objectType is omitted, TABLE and VIEW are searched.",
            "Boundary: broad or ambiguous matches are candidates, not verified schema."
    })
    public AgentToolResult searchObjects(
            @P("Search query parameters") ObjectSearchQuery query,
            @P(value = ToolDescriptionParam.UI_STEP_DESCRIPTION, required = false) String description,
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

    public AgentToolResult searchObjects(ObjectSearchQuery query, InvocationParameters parameters) {
        return searchObjects(query, null, parameters);
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
