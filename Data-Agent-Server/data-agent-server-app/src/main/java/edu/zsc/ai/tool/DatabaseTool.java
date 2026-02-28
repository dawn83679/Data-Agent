package edu.zsc.ai.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.common.constant.ToolMessageConstants;
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
        "[WHAT] List all database names (catalogs) available on a given connection.",
        "[WHEN] Use when the user has not specified a database, or when exploring what databases exist on a connection. Pass connectionId from session context or getMyConnections."
    })
    public String listDatabases(
            @P("The connection id (from session context or getMyConnections result)") Long connectionId,
            InvocationParameters parameters) {
        log.info("{} listDatabases, connectionId={}", ToolMessageConstants.TOOL_LOG_PREFIX_BEFORE, connectionId);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return ToolMessageConstants.USER_CONTEXT_MISSING;
            }
            List<String> databases = databaseService.listDatabases(connectionId, userId);
            if (databases == null || databases.isEmpty()) {
                log.info("{} listDatabases -> {}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE,
                        ToolMessageConstants.EMPTY_NO_DATABASES);
                return ToolMessageConstants.EMPTY_NO_DATABASES;
            }
            log.info("{} listDatabases, result size={}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE, databases.size());
            return databases.toString();
        } catch (Exception e) {
            log.error("{} listDatabases, connectionId={}", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, connectionId, e);
            return e.getMessage();
        }
    }
}
