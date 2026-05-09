package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ToolDescriptionParam;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.sql.model.NamedObjectDetail;
import edu.zsc.ai.agent.tool.sql.model.ObjectQueryItem;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.service.db.DiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

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
            "Value: fetches verified DDL, row counts, indexes, and effective scope for concrete objects.",
            "Use When: objects are narrowed down and SQL generation, execution, join planning, or write-impact review needs real structure.",
            "Preconditions: provide at least one concrete object; omitted connection/database/schema default to current context when available.",
            "Result: one detail item per requested object, with success or failure per item.",
            "Boundary: do not plan SQL against objects whose detail lookup failed."
    })
    public AgentToolResult getObjectDetail(
            @P("List of objects to retrieve details for") List<ObjectQueryItem> objects,
            @P(value = ToolDescriptionParam.UI_STEP_DESCRIPTION, required = false) String description,
            InvocationParameters parameters) {
        log.info("[Tool] getObjectDetail, objectCount={}", CollectionUtils.size(objects));
        if (CollectionUtils.isEmpty(objects)) {
            throw AgentToolExecuteException.invalidInput(
                    ToolNameEnum.GET_OBJECT_DETAIL,
                    "objects list must not be empty. Provide at least one object before retrying getObjectDetail. "
                            + "Do not continue planning until the required object details are available."
            );
        }

        List<ObjectQueryItem> normalizedObjects = objects.stream()
                .map(this::normalizeObjectQueryItem)
                .toList();
        List<NamedObjectDetail> results = discoveryService.getObjectDetails(normalizedObjects);

        log.info("[Tool done] getObjectDetail, requested={}, succeeded={}",
                normalizedObjects.size(), results.stream().filter(NamedObjectDetail::success).count());
        if (results.stream().anyMatch(detail -> !detail.success())) {
            return AgentToolResult.builder()
                    .success(true)
                    .message(buildObjectDetailMessage(results))
                    .result(results)
                    .build();
        }
        return AgentToolResult.success(results, buildObjectDetailSuccessMessage(results));
    }

    public AgentToolResult getObjectDetail(List<ObjectQueryItem> objects, InvocationParameters parameters) {
        return getObjectDetail(objects, null, parameters);
    }

    private ObjectQueryItem normalizeObjectQueryItem(ObjectQueryItem item) {
        if (item == null) {
            return null;
        }
        Long requestConnectionId = RequestContext.getConnectionId();
        Long effectiveConnectionId = item.getConnectionId() != null ? item.getConnectionId() : requestConnectionId;
        String effectiveDatabaseName = item.getDatabaseName();
        String effectiveSchemaName = item.getSchemaName();
        if (Objects.equals(effectiveConnectionId, requestConnectionId)) {
            if (StringUtils.isBlank(effectiveDatabaseName)) {
                effectiveDatabaseName = RequestContext.getCatalog();
            }
            if (StringUtils.isBlank(effectiveSchemaName)) {
                effectiveSchemaName = RequestContext.getSchema();
            }
        }
        return new ObjectQueryItem(
                item.getObjectType(),
                item.getObjectName(),
                effectiveConnectionId,
                effectiveDatabaseName,
                effectiveSchemaName
        );
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
                "Use the returned DDL, row counts, indexes, and effective scope to verify object structure before generating or executing SQL."
        );
    }
}
