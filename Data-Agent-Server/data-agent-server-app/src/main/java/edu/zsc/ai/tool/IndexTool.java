package edu.zsc.ai.tool;

import java.util.List;

import org.springframework.stereotype.Component;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.common.constant.ToolMessageConstants;
import edu.zsc.ai.domain.service.db.IndexService;
import edu.zsc.ai.plugin.model.metadata.IndexMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class IndexTool {

    private final IndexService indexService;

    @Tool({
        "Get the list of all index names and metadata for a specific table.",
        "Use when the user asks what indexes exist on a table or to analyze table performance. Pass connectionId, databaseName, schemaName, tableName from current session context."
    })
    public String getIndexes(
            @P("The exact name of the table") String tableName,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("{} getIndexes, tableName={}, connectionId={}, database={}, schema={}",
                ToolMessageConstants.TOOL_LOG_PREFIX_BEFORE, tableName, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return ToolMessageConstants.USER_CONTEXT_MISSING;
            }
            List<IndexMetadata> indexes = indexService.listIndexes(
                    connectionId,
                    databaseName,
                    schemaName,
                    tableName,
                    userId
            );

            if (indexes == null || indexes.isEmpty()) {
                log.info("{} getIndexes -> {}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE,
                        ToolMessageConstants.EMPTY_NO_INDEXES);
                return ToolMessageConstants.EMPTY_NO_INDEXES;
            }

            log.info("{} getIndexes, result size={}", ToolMessageConstants.TOOL_LOG_PREFIX_DONE, indexes.size());
            return indexes.toString();
        } catch (Exception e) {
            log.error("{} getIndexes, tableName={}", ToolMessageConstants.TOOL_LOG_PREFIX_ERROR, tableName, e);
            return e.getMessage();
        }
    }
}
