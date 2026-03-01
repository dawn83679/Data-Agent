package edu.zsc.ai.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.domain.service.db.ViewService;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.apache.commons.lang3.StringUtils;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class ViewTool {

    private final ViewService viewService;

    @Tool({
        "[WHAT] List all view names in the current database/schema.",
        "[WHEN] Use when the user asks what views exist or wants to query a view. Pass connectionId, databaseName, schemaName from current session context."
    })
    public AgentToolResult getViewNames(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("[Tool] getViewNames, connectionId={}, database={}, schema={}", connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }
            List<String> views = viewService.getViews(connectionId, databaseName, schemaName, userId);
            if (views == null || views.isEmpty()) {
                log.info("[Tool done] getViewNames -> empty");
                return AgentToolResult.empty();
            }
            log.info("[Tool done] getViewNames, result size={}", views.size());
            return AgentToolResult.success(views);
        } catch (Exception e) {
            log.error("[Tool error] getViewNames", e);
            return AgentToolResult.fail(e);
        }
    }

    @Tool({
        "[WHAT] Get the CREATE VIEW DDL for a specific view.",
        "[WHEN] Use when you need to understand what a view selects or its column structure. Pass connectionId, databaseName, schemaName from current session context."
    })
    public AgentToolResult getViewDdl(
            @P("The exact name of the view in the current schema") String viewName,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("[Tool] getViewDdl, viewName={}, connectionId={}, database={}, schema={}", viewName, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }
            String ddl = viewService.getViewDdl(connectionId, databaseName, schemaName, viewName, userId);
            log.info("[Tool done] getViewDdl, viewName={}, ddlLength={}", viewName, StringUtils.isNotBlank(ddl) ? ddl.length() : 0);
            return AgentToolResult.success(ddl);
        } catch (Exception e) {
            log.error("[Tool error] getViewDdl, viewName={}", viewName, e);
            return AgentToolResult.fail(e);
        }
    }
}
