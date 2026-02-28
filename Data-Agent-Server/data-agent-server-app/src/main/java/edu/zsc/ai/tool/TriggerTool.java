package edu.zsc.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.common.constant.ToolMessageConstants;
import edu.zsc.ai.util.JsonUtil;
import edu.zsc.ai.domain.service.db.TriggerService;
import edu.zsc.ai.plugin.model.metadata.TriggerMetadata;
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
public class TriggerTool {

    private final TriggerService triggerService;


    @Tool({
        "[WHAT] List all triggers attached to a specific table.",
        "[WHEN] Use when the user asks about triggers on a table, or when understanding the side effects of a write operation. Pass connectionId, databaseName, schemaName from current session context."
    })
    public String getTriggerNames(
            @P("The exact name of the table") String tableName,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("{} getTriggerNames, tableName={}, connectionId={}, database={}, schema={}", ToolMessageConstants.TOOL_LOG_PREFIX_BEFORE,
                tableName, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return ToolMessageConstants.USER_CONTEXT_MISSING;
            }
            List<TriggerMetadata> triggers = triggerService.getTriggers(
                    connectionId,
                    databaseName,
                    schemaName,
                    tableName,
                    userId
            );

            if (CollectionUtils.isEmpty(triggers)) {
                log.info("{} getTriggerNames -> {}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE,
                        ToolMessageConstants.EMPTY_NO_TRIGGERS);
                return ToolMessageConstants.EMPTY_NO_TRIGGERS;
            }

            log.info("{} getTriggerNames, result size={}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE, triggers.size());
            return JsonUtil.object2json(triggers);
        } catch (Exception e) {
            log.error("{} getTriggerNames", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, e);
            return e.getMessage();
        }
    }

    @Tool({
        "[WHAT] Get the CREATE TRIGGER DDL for a specific trigger.",
        "[WHEN] Use when you need to understand what a trigger does or when it fires. Pass connectionId, databaseName, schemaName from current session context."
    })
    public String getTriggerDdl(
            @P("The exact name of the trigger in the current schema") String triggerName,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("{} getTriggerDdl, triggerName={}, connectionId={}, database={}, schema={}",
                ToolMessageConstants.TOOL_LOG_PREFIX_BEFORE, triggerName, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
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
                    triggerName, StringUtils.isNotBlank(ddl) ? ddl.length() : 0);
            return ddl;
        } catch (Exception e) {
            log.error("{} getTriggerDdl, triggerName={}", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, triggerName, e);
            return e.getMessage();
        }
    }
}
