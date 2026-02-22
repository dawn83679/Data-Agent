package edu.zsc.ai.tool;

import java.util.List;

import org.springframework.stereotype.Component;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.common.constant.ToolMessageConstants;
import edu.zsc.ai.domain.service.db.ViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ViewTool {

    private final ViewService viewService;

    @Tool({
        "Get the list of all view names in the current database/schema.",
        "Use when the user asks what views exist or to explore database views. Pass connectionId, databaseName, schemaName from current session context."
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
            if (userId == null) {
                return ToolMessageConstants.USER_CONTEXT_MISSING;
            }
            List<String> views = viewService.listViews(
                    connectionId,
                    databaseName,
                    schemaName,
                    userId
            );

            if (views == null || views.isEmpty()) {
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
        "Get the DDL (Data Definition Language) statement for a specific view.",
        "Use when the user needs the view definition or CREATE VIEW statement. Pass connectionId, databaseName, schemaName from current session context."
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
            if (userId == null) {
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
                    viewName, ddl != null ? ddl.length() : 0);
            return ddl;
        } catch (Exception e) {
            log.error("{} getViewDdl, viewName={}", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, viewName, e);
            return e.getMessage();
        }
    }
}
