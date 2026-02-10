package edu.zsc.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.domain.service.db.DatabaseService;
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
        "List all database names (catalogs) for a given connection.",
        "Use when exploring which databases exist on a connection, or when the user asks for database list. Pass connectionId from session context or getMyConnections."
    })
    public String listDatabases(
            @P("The connection id (from session context or getMyConnections result)") Long connectionId,
            InvocationParameters parameters) {
        log.info("[Tool before] listDatabases, connectionId={}", connectionId);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return "User context missing.";
            }
            List<String> databases = databaseService.listDatabases(connectionId, userId);
            if (databases == null || databases.isEmpty()) {
                log.info("[Tool done] listDatabases -> EMPTY: No databases found.");
                return "EMPTY: No databases found.";
            }
            log.info("[Tool done] listDatabases, result size={}", databases.size());
            return databases.toString();
        } catch (Exception e) {
            log.error("[Tool error] listDatabases, connectionId={}", connectionId, e);
            return e.getMessage();
        }
    }
}
