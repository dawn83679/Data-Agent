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
            "Returns complete details for one or more database objects in a single call: DDL (structure), ",
            "row count, and index information. Accepts a list of objects — pass multiple objects to ",
            "retrieve all their details at once and save LLM reasoning rounds.",
            "",
            "For TABLE: returns DDL + rowCount + indexes. For VIEW: returns DDL + rowCount (no indexes). ",
            "For FUNCTION/PROCEDURE/TRIGGER: returns DDL only. Call this for EVERY table you plan to ",
            "reference in SQL — the DDL is your ground truth for column names, types, and constraints.",
            "",
            "Each object in the result includes success/error fields — a single object's failure does ",
            "not affect the others. Response includes elapsedMs."
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
