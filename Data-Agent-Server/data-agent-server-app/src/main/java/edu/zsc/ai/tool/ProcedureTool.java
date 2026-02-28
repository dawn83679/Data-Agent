package edu.zsc.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.domain.service.db.ProcedureService;
import edu.zsc.ai.plugin.model.metadata.ProcedureMetadata;
import edu.zsc.ai.tool.model.AgentToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.apache.commons.lang3.StringUtils;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProcedureTool {

    private final ProcedureService procedureService;

    @Tool({
        "[WHAT] List all stored procedures in the current database/schema.",
        "[WHEN] Use when the user asks about procedures or wants to call one. Pass connectionId, databaseName, schemaName from current session context."
    })
    public AgentToolResult getProcedureNames(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("[Tool] getProcedureNames, connectionId={}, database={}, schema={}", connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }
            List<ProcedureMetadata> procedures = procedureService.getProcedures(connectionId, databaseName, schemaName, userId);
            if (procedures == null || procedures.isEmpty()) {
                log.info("[Tool done] getProcedureNames -> empty");
                return AgentToolResult.empty();
            }
            log.info("[Tool done] getProcedureNames, result size={}", procedures.size());
            return AgentToolResult.success(procedures);
        } catch (Exception e) {
            log.error("[Tool error] getProcedureNames", e);
            return AgentToolResult.fail(e);
        }
    }

    @Tool({
        "[WHAT] Get the CREATE PROCEDURE DDL for a specific stored procedure.",
        "[WHEN] Use when you need to understand a procedure's logic, parameters, or behavior. Pass connectionId, databaseName, schemaName from current session context."
    })
    public AgentToolResult getProcedureDdl(
            @P("The exact name of the procedure in the current schema") String procedureName,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("[Tool] getProcedureDdl, procedureName={}, connectionId={}, database={}, schema={}", procedureName, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }
            String ddl = procedureService.getProcedureDdl(connectionId, databaseName, schemaName, procedureName, userId);
            log.info("[Tool done] getProcedureDdl, procedureName={}, ddlLength={}", procedureName, StringUtils.isNotBlank(ddl) ? ddl.length() : 0);
            return AgentToolResult.success(ddl);
        } catch (Exception e) {
            log.error("[Tool error] getProcedureDdl, procedureName={}", procedureName, e);
            return AgentToolResult.fail(e);
        }
    }
}
