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

@AgentTool
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchObjectsTool {
    private final DiscoveryService discoveryService;

    @Tool({
            "价值：按 SQL 通配模式查找候选表、视图和其他数据库对象。",
            "使用时机：对象范围尚未精确，但连接、数据库或 schema 范围已经足够窄，可以做轻量发现。",
            "前置条件：objectNamePattern 必填。databaseNamePattern 需要同时提供 connectionId。schemaNamePattern 需要同时提供 connectionId 和 databaseNamePattern。",
            "结果：最多返回 100 条匹配；未指定 objectType 时检索 TABLE 和 VIEW。",
            "边界：宽泛或有歧义的匹配只能视为候选，不是已验证 schema。"
    })
    public AgentToolResult searchObjects(
            @P("对象搜索参数") ObjectSearchQuery query,
            @P(ToolDescriptionParam.UI_STEP_DESCRIPTION) String description,
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
                    "schemaNamePattern 需要同时指定 databaseNamePattern。重试 searchObjects 前先补充 databaseNamePattern。"
                            + "范围有效前不要继续对象发现。"
            );
        }
        if (StringUtils.isNotBlank(databaseNamePattern) && connectionId == null) {
            throw AgentToolExecuteException.invalidInput(
                    ToolNameEnum.SEARCH_OBJECTS,
                    "databaseNamePattern 需要同时指定 connectionId。重试 searchObjects 前先补充 connectionId。"
                            + "范围有效前不要继续对象发现。"
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
        String baseMessage = "对象搜索在范围 " + scope + " 上遇到失败。失败信息：" + errorSummary + "。";
        if (CollectionUtils.isNotEmpty(response.results())) {
            return ToolMessageSupport.sentence(
                    baseMessage,
                    "继续前使用 askUserQuestion 询问用户明确目标范围。"
            );
        }

        return ToolMessageSupport.sentence(
                baseMessage,
                "继续前使用 askUserQuestion 询问用户明确目标范围。"
        );
    }

    private String buildSearchSuccessMessage(ObjectSearchResponse response,
                                             String objectNamePattern,
                                             Long connectionId,
                                             String databaseNamePattern,
                                             String schemaNamePattern) {
        String scope = buildScopeLabel(objectNamePattern, connectionId, databaseNamePattern, schemaNamePattern);
        String truncation = response.truncated()
                ? " 结果已被截断，假设已看到全部匹配前必须先缩小搜索范围。"
                : "";
        if (isFuzzyPattern(objectNamePattern)) {
            return ToolMessageSupport.sentence(
                    "对象搜索在范围 " + scope + " 中找到 " + response.totalCount() + " 个候选。",
                    "继续前使用 askUserQuestion 询问用户缩小目标对象范围。" + truncation
            );
        }
        return ToolMessageSupport.sentence(
                "对象搜索在范围 " + scope + " 中找到 " + response.totalCount() + " 个候选。",
                "继续前使用 askUserQuestion 询问用户缩小目标对象范围。" + truncation
        );
    }

    private String buildEmptySearchMessage(String objectNamePattern,
                                           Long connectionId,
                                           String databaseNamePattern,
                                           String schemaNamePattern) {
        return ToolMessageSupport.sentence(
                "对象搜索在范围 "
                        + buildScopeLabel(objectNamePattern, connectionId, databaseNamePattern, schemaNamePattern) + " 中没有匹配结果。",
                "继续前使用 askUserQuestion 询问用户明确目标。"
        );
    }

    private String buildScopeLabel(String objectNamePattern,
                                   Long connectionId,
                                   String databaseNamePattern,
                                   String schemaNamePattern) {
        StringBuilder builder = new StringBuilder();
        builder.append("对象名模式=")
                .append(StringUtils.defaultIfBlank(objectNamePattern, "<空>"));
        if (connectionId != null) {
            builder.append(", connectionId=").append(connectionId);
        }
        if (StringUtils.isNotBlank(databaseNamePattern)) {
            builder.append(", 数据库模式=").append(databaseNamePattern);
        }
        if (StringUtils.isNotBlank(schemaNamePattern)) {
            builder.append(", schema模式=").append(schemaNamePattern);
        }
        return builder.toString();
    }

    private boolean isFuzzyPattern(String objectNamePattern) {
        return StringUtils.contains(objectNamePattern, '%') || StringUtils.contains(objectNamePattern, '_');
    }
}
