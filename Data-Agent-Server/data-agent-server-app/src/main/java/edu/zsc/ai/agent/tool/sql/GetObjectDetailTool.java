package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.sql.model.NamedObjectDetail;
import edu.zsc.ai.agent.tool.sql.model.ObjectQueryItem;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.domain.service.db.DiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Batch retrieve DDL, row count, and indexes for tables/views.
 * Explorer SubAgent uses this for schema structure discovery.
 */
@AgentTool
@Slf4j
@Component
@RequiredArgsConstructor
public class GetObjectDetailTool {

    private final DiscoveryService discoveryService;

    @Tool({
            "Returns DDL, row count, and indexes per object. Pass multiple objects in one call.",
            "TABLE: DDL + rowCount + indexes. VIEW: DDL + rowCount. Each object has success/error."
    })
    public AgentToolResult getObjectDetail(
            @P("List of objects to retrieve details for") List<ObjectQueryItem> objects,
            InvocationParameters parameters) {
        log.info("[Tool] getObjectDetail, objectCount={}", CollectionUtils.size(objects));
        if (CollectionUtils.isEmpty(objects)) {
            throw AgentToolExecuteException.invalidInput(
                    ToolNameEnum.GET_OBJECT_DETAIL,
                    "objects list must not be empty."
            );
        }

        List<NamedObjectDetail> results = discoveryService.getObjectDetails(objects);

        log.info("[Tool done] getObjectDetail, requested={}, succeeded={}",
                objects.size(), results.stream().filter(NamedObjectDetail::success).count());
        if (results.stream().anyMatch(detail -> !detail.success())) {
            return AgentToolResult.builder()
                    .success(true)
                    .message(buildObjectDetailMessage(results))
                    .result(results)
                    .build();
        }
        return AgentToolResult.success(results);
    }

    private String buildObjectDetailMessage(List<NamedObjectDetail> results) {
        List<String> failedObjects = results.stream()
                .filter(detail -> !detail.success())
                .map(detail -> StringUtils.defaultIfBlank(detail.objectName(), "unknown_object"))
                .toList();
        String failedSummary = String.join(", ", failedObjects);
        if (failedObjects.size() < results.size()) {
            return "Object detail lookup failed for: "
                    + failedSummary
                    + ". Ask the user whether these objects are still required before continuing. Do not continue object discovery until the user replies.";
        }
        return "Object detail lookup failed for all requested objects: "
                + failedSummary
                + ". Ask the user whether to retry with another object or connection. Do not continue object discovery until the user replies.";
    }
}
