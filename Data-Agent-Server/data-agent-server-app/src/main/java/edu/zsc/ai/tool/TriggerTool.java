package edu.zsc.ai.tool;

import java.util.List;

import org.springframework.stereotype.Component;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.common.constant.ToolMessageConstants;
import edu.zsc.ai.domain.service.db.TriggerService;
import edu.zsc.ai.plugin.model.metadata.TriggerMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class TriggerTool {

    private final TriggerService triggerService;

    @Tool({
        "Get the list of all trigger names and metadata for a specific table.",
        "Use when the user asks what triggers exist on a table. Pass connectionId, databaseName, schemaName, tableName from current session context."
    })
    public String getTriggerNames(
            @P("The exact name of the table") String tableName,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("{} getTriggerNames, tableName={}, connectionId={}, database={}, schema={}",
                ToolMessageConstants.TOOL_LOG_PREFIX_BEFORE, tableName, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return ToolMessageConstants.USER_CONTEXT_MISSING;
            }
            List<TriggerMetadata> triggers = triggerService.listTriggers(
                    connectionId,
                    databaseName,
                    schemaName,
                    tableName,
                    userId
            );

            if (triggers == null || triggers.isEmpty()) {
                log.info("{} getTriggerNames -> {}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE,
                        ToolMessageConstants.EMPTY_NO_TRIGGERS);
                return ToolMessageConstants.EMPTY_NO_TRIGGERS;
            }

            log.info("{} getTriggerNames, result size={}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE, triggers.size());
            return triggers.toString();
        } catch (Exception e) {
            log.error("{} getTriggerNames, tableName={}", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, tableName, e);
            return e.getMessage();
        }
    }

    @Tool({
        "Get the DDL (Data Definition Language) statement for a specific trigger.",
        "Use when the user needs the trigger definition or CREATE TRIGGER statement. Pass connectionId, databaseName, schemaName from current session context."
    })
    public String getTriggerDdl(
            @P("The exact name of the trigger") String triggerName,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("{} getTriggerDdl, triggerName={}, connectionId={}, database={}, schema={}",
                ToolMessageConstants.TOOL_LOG_PREFIX_BEFORE, triggerName, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return ToolMessageConstants.USER_CONTEXT_MISSING;
            }
            String ddl = triggerService.getTriggerDdl(
                    connectionId,
                    databaseName,
                    schemaName,
                    triggerName,
                    userId
            );

            log.info("{} getTriggerDdl, triggerName={}, ddlLength={}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE,
                    triggerName, ddl != null ? ddl.length() : 0);
            return ddl;
        } catch (Exception e) {
            log.error("{} getTriggerDdl, triggerName={}", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, triggerName, e);
            return e.getMessage();
        }
    }
}
