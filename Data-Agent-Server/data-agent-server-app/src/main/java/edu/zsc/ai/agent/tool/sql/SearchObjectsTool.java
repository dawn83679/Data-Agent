package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.guard.ExplorerConnectionScopeGuard;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
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
            "Pattern search across all connections. Use SQL wildcards: %order%, %user_%.",
            "Required: objectNamePattern. Optional: connectionId, databaseName, schemaName.",
            "databaseName requires connectionId; schemaName requires connectionId + databaseName.",
            "Results capped at 100. objectType omitted = TABLE + VIEW."
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
                    "schemaName requires databaseName to be specified."
            );
        }
        if (StringUtils.isNotBlank(databaseName) && connectionId == null) {
            throw AgentToolExecuteException.invalidInput(
                    ToolNameEnum.SEARCH_OBJECTS,
                    "databaseName requires connectionId to be specified."
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
                    .message(buildSearchMessage(response))
                    .result(response)
                    .build();
        }

        if (CollectionUtils.isEmpty(response.results())) {
            log.info("[Tool done] searchObjects -> empty");
            return AgentToolResult.empty();
        }

        log.info("[Tool done] searchObjects, resultCount={}, truncated={}",
                response.totalCount(), response.truncated());
        return AgentToolResult.success(response);
    }

    private String buildSearchMessage(ObjectSearchResponse response) {
        String errorSummary = String.join("; ", response.errors());
        if (CollectionUtils.isNotEmpty(response.results())) {
            return "Search completed with scope errors: "
                    + errorSummary
                    + ". Ask the user whether to continue with the available matches or change the connection/scope. Do not continue object discovery until the user replies.";
        }

        return "Search could not reliably complete: "
                + errorSummary
                + ". Ask the user whether to retry with another connection or scope. Do not continue object discovery until the user replies.";
    }
}
