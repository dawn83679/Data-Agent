package edu.zsc.ai.agent.subagent.explorer;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import edu.zsc.ai.agent.subagent.contract.ExploreObject;
import edu.zsc.ai.agent.subagent.contract.ExploreObjectScoreSupport;
import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.sql.model.NamedObjectDetail;
import edu.zsc.ai.agent.tool.sql.model.ObjectQueryItem;
import edu.zsc.ai.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Collects structured data from getObjectDetail tool results during Explorer SubAgent execution.
 * Used to build SchemaSummary.objects without regex-parsing LLM text.
 */
@Slf4j
public class ExplorerToolResultCollector {

    private final List<ExploreObject> objects = new CopyOnWriteArrayList<>();

    /**
     * Handles tool execution results. Parses and collects object details when tool is getObjectDetail.
     */
    public void onToolExecuted(ToolExecutionRequest request, Object result) {
        String toolName = request != null ? request.name() : null;
        log.debug("[Collector] onToolExecuted: tool={}, resultLen={}", toolName, result != null ? result.toString().length() : 0);
        if (!"getObjectDetail".equals(toolName)) return;
        if (result == null) return;

        try {
            AgentToolResult agentResult = parseAsAgentToolResult(result);
            if (agentResult == null || !agentResult.isSuccess() || agentResult.getResult() == null) return;

            Object data = agentResult.getResult();
            List<NamedObjectDetail> details = parseAsNamedObjectDetails(data);
            if (details == null) return;
            List<ObjectQueryItem> queryItems = parseQueryItems(request);

            for (int index = 0; index < details.size(); index++) {
                NamedObjectDetail d = details.get(index);
                if (!d.success() || d.ddl() == null) continue;
                ObjectQueryItem queryItem = index < queryItems.size() ? queryItems.get(index) : null;
                ExploreObject exploreObject = toExploreObject(d, queryItem);
                if (exploreObject != null && objects.stream().noneMatch(existing -> sameObject(existing, exploreObject))) {
                    objects.add(exploreObject);
                    log.debug("[Collector] collected object: {}", exploreObject.getObjectName());
                }
            }
        } catch (Exception e) {
            log.warn("[ExplorerToolResultCollector] Failed to parse getObjectDetail result: {}", e.getMessage());
        }
    }

    /**
     * Builds SchemaSummary and clears the collector for reuse.
     */
    public SchemaSummary buildAndClear(String rawResponse) {
        List<ExploreObject> copy = new ArrayList<>(objects);
        ExploreObjectScoreSupport.normalizeAndSort(copy);
        log.info("[Collector] built SchemaSummary: {} object(s)", copy.size());
        objects.clear();
        return SchemaSummary.builder()
                .rawResponse(rawResponse != null ? rawResponse : "")
                .objects(copy)
                .build();
    }

    private AgentToolResult parseAsAgentToolResult(Object result) {
        if (result instanceof AgentToolResult) {
            return (AgentToolResult) result;
        }
        if (result instanceof String) {
            return JsonUtil.json2Object((String) result, AgentToolResult.class);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<NamedObjectDetail> parseAsNamedObjectDetails(Object data) {
        if (data instanceof List) {
            List<?> list = (List<?>) data;
            if (list.isEmpty()) return List.of();
            Object first = list.get(0);
            if (first instanceof NamedObjectDetail) {
                return (List<NamedObjectDetail>) list;
            }
            if (first instanceof java.util.Map) {
                return list.stream()
                        .map(o -> JsonUtil.object2json(o))
                        .map(json -> JsonUtil.json2Object(json, NamedObjectDetail.class))
                        .toList();
            }
        }
        if (data instanceof String) {
            return JsonUtil.json2Object((String) data, new TypeReference<List<NamedObjectDetail>>() {});
        }
        return null;
    }

    private List<ObjectQueryItem> parseQueryItems(ToolExecutionRequest request) {
        if (request == null || request.arguments() == null || request.arguments().isBlank()) {
            return List.of();
        }
        try {
            var parsed = JsonUtil.json2Object(request.arguments(), new TypeReference<java.util.Map<String, Object>>() {});
            Object objectsValue = parsed != null ? parsed.get("objects") : null;
            if (objectsValue == null) return List.of();
            return JsonUtil.json2Object(JsonUtil.object2json(objectsValue), new TypeReference<List<ObjectQueryItem>>() {});
        } catch (Exception e) {
            log.warn("[ExplorerToolResultCollector] Failed to parse getObjectDetail arguments: {}", e.getMessage());
            return List.of();
        }
    }

    private ExploreObject toExploreObject(NamedObjectDetail detail, ObjectQueryItem queryItem) {
        String effectiveCatalog = detail.databaseName() != null
                ? detail.databaseName()
                : queryItem != null ? queryItem.getDatabaseName() : null;
        String effectiveSchema = detail.schemaName() != null
                ? detail.schemaName()
                : queryItem != null ? queryItem.getSchemaName() : null;
        return ExploreObject.builder()
                .catalog(effectiveCatalog)
                .schema(effectiveSchema)
                .objectName(detail.objectName())
                .objectType(detail.objectType())
                .objectDdl(detail.ddl())
                .relevanceScore(ExploreObjectScoreSupport.DEFAULT_RELEVANCE_SCORE)
                .build();
    }

    private boolean sameObject(ExploreObject left, ExploreObject right) {
        return java.util.Objects.equals(left.getCatalog(), right.getCatalog())
                && java.util.Objects.equals(left.getSchema(), right.getSchema())
                && java.util.Objects.equals(left.getObjectName(), right.getObjectName())
                && java.util.Objects.equals(left.getObjectType(), right.getObjectType());
    }
}
