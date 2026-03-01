package edu.zsc.ai.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.domain.service.db.TriggerService;
import edu.zsc.ai.plugin.model.metadata.TriggerMetadata;
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
public class TriggerTool {

    private final TriggerService triggerService;

    @Tool({
        "[WHAT] List all triggers attached to a specific table.",
        "[WHEN] Use when the user asks about triggers on a table, or when understanding the side effects of a write operation. Pass connectionId, databaseName, schemaName from current session context."
    })
    public AgentToolResult getTriggerNames(
            @P("The exact name of the table") String tableName,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("[Tool] getTriggerNames, tableName={}, connectionId={}, database={}, schema={}", tableName, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }
            List<TriggerMetadata> triggers = triggerService.getTriggers(connectionId, databaseName, schemaName, tableName, userId);
            if (triggers == null || triggers.isEmpty()) {
                log.info("[Tool done] getTriggerNames -> empty");
                return AgentToolResult.empty();
            }
            log.info("[Tool done] getTriggerNames, result size={}", triggers.size());
            return AgentToolResult.success(triggers);
        } catch (Exception e) {
            log.error("[Tool error] getTriggerNames", e);
            return AgentToolResult.fail(e);
        }
    }

    @Tool({
        "[WHAT] Get the CREATE TRIGGER DDL for a specific trigger.",
        "[WHEN] Use when you need to understand what a trigger does or when it fires. Pass connectionId, databaseName, schemaName from current session context."
    })
    public AgentToolResult getTriggerDdl(
            @P("The exact name of the trigger in the current schema") String triggerName,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("[Tool] getTriggerDdl, triggerName={}, connectionId={}, database={}, schema={}", triggerName, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }
            String ddl = triggerService.getTriggerDdl(connectionId, databaseName, schemaName, triggerName, userId);
            log.info("[Tool done] getTriggerDdl, triggerName={}, ddlLength={}", triggerName, StringUtils.isNotBlank(ddl) ? ddl.length() : 0);
            return AgentToolResult.success(ddl);
        } catch (Exception e) {
            log.error("[Tool error] getTriggerDdl, triggerName={}", triggerName, e);
            return AgentToolResult.fail(e);
        }
    }
}
