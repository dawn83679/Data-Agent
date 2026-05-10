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

@AgentTool
@Slf4j
@Component
@RequiredArgsConstructor
public class GetObjectDetailTool {

    private final DiscoveryService discoveryService;

    @Tool({
            "价值：获取具体对象的 DDL、行数、索引和生效范围。",
            "使用时机：对象已缩小到具体候选，且生成 SQL、执行 SQL、规划 JOIN 或评估写入影响需要真实结构。",
            "前置条件：至少提供一个具体对象；缺省的连接、数据库或 schema 会在可用时使用当前上下文。",
            "结果：每个请求对象返回一条详情，并标记该对象成功或失败。",
            "边界：详情查询失败的对象不能用于 SQL 规划。"
    })
    public AgentToolResult getObjectDetail(
            @P("要获取详情的对象列表") List<ObjectQueryItem> objects,
            @P(ToolDescriptionParam.UI_STEP_DESCRIPTION) String description,
            InvocationParameters parameters) {
        log.info("[Tool] getObjectDetail, objectCount={}", CollectionUtils.size(objects));
        if (CollectionUtils.isEmpty(objects)) {
            throw AgentToolExecuteException.invalidInput(
                    ToolNameEnum.GET_OBJECT_DETAIL,
                    "objects 列表不能为空。重试 getObjectDetail 前至少提供一个对象。"
                            + "必要对象详情可用前不要继续规划。"
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
                .map(detail -> StringUtils.defaultIfBlank(detail.objectName(), "未知对象")
                        + " (" + StringUtils.defaultIfBlank(detail.error(), "未知错误") + ")")
                .toList();
        String failedSummary = String.join(", ", failedObjects);
        if (failedObjects.size() < results.size()) {
            return ToolMessageSupport.sentence(
                    "对象详情只有部分可用。失败对象：" + failedSummary + "。",
                    "不要假设失败对象的结构。",
                    "继续前询问用户这些对象是否仍然需要。",
                    ToolMessageSupport.DO_NOT_CONTINUE_OBJECT_DISCOVERY_UNTIL_USER_REPLIES
            );
        }
        return ToolMessageSupport.sentence(
                "所有请求对象的详情查询都失败：" + failedSummary + "。",
                ToolMessageSupport.askUserWhether("改用其他对象或连接重试"),
                ToolMessageSupport.DO_NOT_CONTINUE_OBJECT_DISCOVERY_UNTIL_USER_REPLIES
        );
    }

    private String buildObjectDetailSuccessMessage(List<NamedObjectDetail> results) {
        List<String> successfulObjects = results.stream()
                .filter(NamedObjectDetail::success)
                .map(detail -> StringUtils.defaultIfBlank(detail.objectName(), "未知对象"))
                .limit(3)
                .toList();
        String suffix = results.size() > successfulObjects.size() ? ", ..." : "";
        return ToolMessageSupport.sentence(
                "已获取这些对象的详情：" + String.join(", ", successfulObjects) + suffix + "。",
                "生成或执行 SQL 前，使用返回的 DDL、行数、索引和生效范围验证对象结构。"
        );
    }
}
