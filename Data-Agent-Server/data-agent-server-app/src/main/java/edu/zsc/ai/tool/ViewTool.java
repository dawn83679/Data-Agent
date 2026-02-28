package edu.zsc.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.common.constant.ToolMessageConstants;
import edu.zsc.ai.domain.service.db.ViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import java.util.Objects;

import java.util.List;


@Component
@Slf4j
@RequiredArgsConstructor
public class ViewTool {

    private final ViewService viewService;


    @Tool({
        "[WHAT] List all view names in the current database/schema.",
        "[WHEN] Use when the user asks what views exist or wants to query a view. Pass connectionId, databaseName, schemaName from current session context."
    })
    public String getViewNames(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("{} getViewNames, connectionId={}, database={}, schema={}", ToolMessageConstants.TOOL_LOG_PREFIX_BEFORE,
                connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return ToolMessageConstants.USER_CONTEXT_MISSING;
            }
            List<String> views = viewService.getViews(
                    connectionId,
                    databaseName,
                    schemaName,
                    userId
            );

            if (CollectionUtils.isEmpty(views)) {
                log.info("{} getViewNames -> {}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE,
                        ToolMessageConstants.EMPTY_NO_VIEWS);
                return ToolMessageConstants.EMPTY_NO_VIEWS;
            }

            log.info("{} getViewNames, result size={}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE, views.size());
            return views.toString();
        } catch (Exception e) {
            log.error("{} getViewNames", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, e);
            return e.getMessage();
        }
    }

    @Tool({
        "[WHAT] Get the CREATE VIEW DDL for a specific view.",
        "[WHEN] Use when you need to understand what a view selects or its column structure. Pass connectionId, databaseName, schemaName from current session context."
    })
    public String getViewDdl(
            @P("The exact name of the view in the current schema") String viewName,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("{} getViewDdl, viewName={}, connectionId={}, database={}, schema={}",
                ToolMessageConstants.TOOL_LOG_PREFIX_BEFORE, viewName, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return ToolMessageConstants.USER_CONTEXT_MISSING;
            }
            String ddl = viewService.getViewDdl(
                    connectionId,
                    databaseName,
                    schemaName,
                    viewName,
                    userId
            );

            log.info("{} getViewDdl, viewName={}, ddlLength={}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE,
                    viewName, StringUtils.isNotBlank(ddl) ? ddl.length() : 0);
            return ddl;
        } catch (Exception e) {
            log.error("{} getViewDdl, viewName={}", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, viewName, e);
            return e.getMessage();
        }
    }
}
