package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ToolContext;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.sql.model.NamedObjectDetail;
import edu.zsc.ai.agent.tool.sql.model.ObjectQueryItem;
import edu.zsc.ai.domain.service.db.DiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@AgentTool
@Slf4j
@RequiredArgsConstructor
public class SchemaDetailTool {

    private final DiscoveryService discoveryService;

    @Tool({
            "Returns DDL, row count, and indexes for one or more database objects in a single call. ",
            "Pass multiple objects to batch-retrieve and save LLM rounds.",
            "",
            "TABLE: DDL + rowCount + indexes. VIEW: DDL + rowCount. FUNCTION/PROCEDURE/TRIGGER: DDL only.",
            "Each object has independent success/error — one failure doesn't affect others.",
            "",
            "Use when: need DDL, column types, constraints, or row counts before writing SQL.",
            "Skip when: instructions already provide complete DDL for all target tables."
    })
    public AgentToolResult getObjectDetail(
            @P("List of objects to retrieve details for") List<ObjectQueryItem> objects,
            InvocationParameters parameters) {
        try (var ctx = ToolContext.from(parameters)) {
            log.info("[Tool] getObjectDetail (SchemaDetailTool), objectCount={}", CollectionUtils.size(objects));
            if (CollectionUtils.isEmpty(objects)) {
                return AgentToolResult.fail("objects list must not be empty.");
            }

            List<NamedObjectDetail> results = discoveryService.getObjectDetails(objects);

            log.info("[Tool done] getObjectDetail (SchemaDetailTool), requested={}, succeeded={}",
                    objects.size(), results.stream().filter(NamedObjectDetail::success).count());
            return ctx.timed(AgentToolResult.success(results));
        } catch (Exception e) {
            log.error("[Tool error] getObjectDetail (SchemaDetailTool)", e);
            String errorMsg = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
            return AgentToolResult.fail("Failed to get object details: " + errorMsg);
        }
    }
}
