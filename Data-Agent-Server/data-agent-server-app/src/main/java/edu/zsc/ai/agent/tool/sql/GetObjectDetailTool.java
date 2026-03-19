package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
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
            "Value: fetches verified DDL, row counts, and indexes so SQL generation uses real structure instead of assumptions.",
            "Use When: call after the target objects are narrowed down and before generating or executing SQL against them.",
            "Preconditions: provide at least one concrete object. Batch multiple objects in one call when comparing or planning joins.",
            "After Success: use the returned DDL, row counts, and indexes to validate joins, filters, limits, and write impact before moving forward.",
            "After Partial Success: continue only with objects whose detail lookup succeeded; askUserQuestion or retry if failed objects may still matter.",
            "After Failure: correct the object identifiers or scope and retry. Do not plan SQL against unknown structure.",
            "Relation: usually after searchObjects and before callingPlannerSubAgent, executeSelectSql, or executeNonSelectSql for write flows."
    })
    public AgentToolResult getObjectDetail(
            @P("List of objects to retrieve details for") List<ObjectQueryItem> objects,
            InvocationParameters parameters) {
        log.info("[Tool] getObjectDetail, objectCount={}", CollectionUtils.size(objects));
        if (CollectionUtils.isEmpty(objects)) {
            throw AgentToolExecuteException.invalidInput(
                    ToolNameEnum.GET_OBJECT_DETAIL,
                    "objects list must not be empty. Provide at least one object before retrying getObjectDetail. "
                            + "Do not continue planning until the required object details are available."
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
        return AgentToolResult.success(results, buildObjectDetailSuccessMessage(results));
    }

    private String buildObjectDetailMessage(List<NamedObjectDetail> results) {
        List<String> failedObjects = results.stream()
                .filter(detail -> !detail.success())
                .map(detail -> StringUtils.defaultIfBlank(detail.objectName(), "unknown_object")
                        + " (" + StringUtils.defaultIfBlank(detail.error(), "unknown error") + ")")
                .toList();
        String failedSummary = String.join(", ", failedObjects);
        if (failedObjects.size() < results.size()) {
            return ToolMessageSupport.sentence(
                    "Object detail lookup is only partially available. Failed objects: " + failedSummary + ".",
                    "Do not assume the structure of failed objects.",
                    "Ask the user whether these objects are still required before continuing.",
                    ToolMessageSupport.DO_NOT_CONTINUE_OBJECT_DISCOVERY_UNTIL_USER_REPLIES
            );
        }
        return ToolMessageSupport.sentence(
                "Object detail lookup failed for all requested objects: " + failedSummary + ".",
                ToolMessageSupport.askUserWhether("retry with another object or connection"),
                ToolMessageSupport.DO_NOT_CONTINUE_OBJECT_DISCOVERY_UNTIL_USER_REPLIES
        );
    }

    private String buildObjectDetailSuccessMessage(List<NamedObjectDetail> results) {
        List<String> successfulObjects = results.stream()
                .filter(NamedObjectDetail::success)
                .map(detail -> StringUtils.defaultIfBlank(detail.objectName(), "unknown_object"))
                .limit(3)
                .toList();
        String suffix = results.size() > successfulObjects.size() ? ", ..." : "";
        return ToolMessageSupport.sentence(
                "Object details are available for " + String.join(", ", successfulObjects) + suffix + ".",
                "Use the returned DDL, row counts, and indexes to verify object structure before generating or executing SQL."
        );
    }
}
