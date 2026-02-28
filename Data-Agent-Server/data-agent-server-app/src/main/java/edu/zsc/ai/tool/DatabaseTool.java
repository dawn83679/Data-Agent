package edu.zsc.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.domain.service.db.DatabaseService;
import edu.zsc.ai.tool.model.AgentToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseTool {

    private final DatabaseService databaseService;

    @Tool({
        "[WHAT] List all database names (catalogs) available on a given connection.",
        "[WHEN] Use when the user has not specified a database, or when exploring what databases exist on a connection. Pass connectionId from session context or getMyConnections."
    })
    public AgentToolResult listDatabases(
            @P("The connection id (from session context or getMyConnections result)") Long connectionId,
            InvocationParameters parameters) {
        log.info("[Tool] listDatabases, connectionId={}", connectionId);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return AgentToolResult.noContext();
            }
            List<String> databases = databaseService.listDatabases(connectionId, userId);
            if (databases == null || databases.isEmpty()) {
                log.info("[Tool done] listDatabases -> empty");
                return AgentToolResult.empty();
            }
            log.info("[Tool done] listDatabases, result size={}", databases.size());
            return AgentToolResult.success(databases);
        } catch (Exception e) {
            log.error("[Tool error] listDatabases, connectionId={}", connectionId, e);
            return AgentToolResult.fail(e);
        }
    }
}
