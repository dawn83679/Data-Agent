package edu.zsc.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.common.constant.ToolMessageConstants;
import edu.zsc.ai.util.JsonUtil;
import edu.zsc.ai.domain.service.db.ProcedureService;
import edu.zsc.ai.plugin.model.metadata.ProcedureMetadata;
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
public class ProcedureTool {

    private final ProcedureService procedureService;


    @Tool({
        "[WHAT] List all stored procedures in the current database/schema.",
        "[WHEN] Use when the user asks about procedures or wants to call one. Pass connectionId, databaseName, schemaName from current session context."
    })
    public String getProcedureNames(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("{} getProcedureNames, connectionId={}, database={}, schema={}", ToolMessageConstants.TOOL_LOG_PREFIX_BEFORE,
                connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return ToolMessageConstants.USER_CONTEXT_MISSING;
            }
            List<ProcedureMetadata> procedures = procedureService.getProcedures(
                    connectionId,
                    databaseName,
                    schemaName,
                    userId
            );

            if (CollectionUtils.isEmpty(procedures)) {
                log.info("{} getProcedureNames -> {}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE,
                        ToolMessageConstants.EMPTY_NO_PROCEDURES);
                return ToolMessageConstants.EMPTY_NO_PROCEDURES;
            }

            log.info("{} getProcedureNames, result size={}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE, procedures.size());
            return JsonUtil.object2json(procedures);
        } catch (Exception e) {
            log.error("{} getProcedureNames", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, e);
            return e.getMessage();
        }
    }

    @Tool({
        "[WHAT] Get the CREATE PROCEDURE DDL for a specific stored procedure.",
        "[WHEN] Use when you need to understand a procedure's logic, parameters, or behavior. Pass connectionId, databaseName, schemaName from current session context."
    })
    public String getProcedureDdl(
            @P("The exact name of the procedure in the current schema") String procedureName,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("{} getProcedureDdl, procedureName={}, connectionId={}, database={}, schema={}",
                ToolMessageConstants.TOOL_LOG_PREFIX_BEFORE, procedureName, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return ToolMessageConstants.USER_CONTEXT_MISSING;
            }
            String ddl = procedureService.getProcedureDdl(
                    connectionId,
                    databaseName,
                    schemaName,
                    procedureName,
                    userId
            );

            log.info("{} getProcedureDdl, procedureName={}, ddlLength={}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE,
                    procedureName, StringUtils.isNotBlank(ddl) ? ddl.length() : 0);
            return ddl;
        } catch (Exception e) {
            log.error("{} getProcedureDdl, procedureName={}", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, procedureName, e);
            return e.getMessage();
        }
    }
}
