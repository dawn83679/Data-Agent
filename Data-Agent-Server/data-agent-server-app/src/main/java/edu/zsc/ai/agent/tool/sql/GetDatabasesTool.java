package edu.zsc.ai.agent.tool.sql;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.domain.service.db.DatabaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AgentTool
@Slf4j
@Component
@RequiredArgsConstructor
public class GetDatabasesTool {

    private final DatabaseService databaseService;

    @Tool({
            "Value: returns the databases (catalogs) available on a specific connection.",
            "Use When: you already know the target connection (from the runtime context) and need to discover its databases before narrowing scope.",
            "After Success: pick the relevant database and proceed with searchObjects or getSchemas.",
            "After Failure: the connection may be unreachable; ask the user to verify the connection."
    })
    public AgentToolResult getDatabases(
            @P("The connection ID to get databases for") Long connectionId,
            InvocationParameters parameters) {
        log.info("[Tool] getDatabases connectionId={}", connectionId);
        try {
            List<String> databases = databaseService.getDatabases(connectionId);
            if (CollectionUtils.isEmpty(databases)) {
                log.info("[Tool done] getDatabases -> empty");
                return AgentToolResult.empty(
                        "No databases found on connection " + connectionId + ".");
            }
            log.info("[Tool done] getDatabases connectionId={}, count={}", connectionId, databases.size());
            return AgentToolResult.success(databases,
                    "Found " + databases.size() + " database(s) on connection " + connectionId + ".");
        } catch (Exception e) {
            log.warn("[Tool] getDatabases failed for connectionId={}: {}", connectionId, e.getMessage());
            return AgentToolResult.fail(
                    "Failed to get databases on connection " + connectionId + ": " + e.getMessage());
        }
    }
}
