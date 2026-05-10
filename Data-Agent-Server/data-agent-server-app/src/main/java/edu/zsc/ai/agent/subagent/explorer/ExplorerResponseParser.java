package edu.zsc.ai.agent.subagent.explorer;

import com.fasterxml.jackson.databind.JsonNode;
import edu.zsc.ai.agent.subagent.contract.ExploreObjectScoreSupport;
import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import edu.zsc.ai.util.JsonUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Parses the Explorer model response as a structured SchemaSummary.
 * The model is expected to return a single JSON object with summaryText, objects, and rawResponse.
 */
public final class ExplorerResponseParser {

    private ExplorerResponseParser() {
    }

    public static SchemaSummary parse(String responseText) {
        if (StringUtils.isBlank(responseText)) {
            throw new IllegalArgumentException("Explorer 响应为空");
        }

        String json = extractJsonObject(responseText);
        SchemaSummary summary;
        try {
            summary = JsonUtil.json2Object(json, SchemaSummary.class);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Explorer 响应必须匹配预期 JSON schema", e);
        }
        if (StringUtils.isBlank(summary.getSummaryText())
                && CollectionUtils.isEmpty(summary.getObjects())
                && StringUtils.isBlank(summary.getRawResponse())) {
            throw new IllegalArgumentException("Explorer 响应 JSON 不包含 summaryText、objects 或 rawResponse");
        }
        ExploreObjectScoreSupport.normalizeAndSort(summary.getObjects());
        return summary;
    }

    private static String extractJsonObject(String responseText) {
        String trimmed = StringUtils.trim(responseText);
        if (StringUtils.startsWith(trimmed, "```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline >= 0) {
                trimmed = StringUtils.trim(trimmed.substring(firstNewline + 1));
            }
            if (StringUtils.endsWith(trimmed, "```")) {
                trimmed = StringUtils.trim(trimmed.substring(0, trimmed.lastIndexOf("```")));
            }
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("Explorer 响应必须包含 JSON 对象");
        }

        String candidate = trimmed.substring(start, end + 1);
        JsonNode node = JsonUtil.readTree(candidate);
        if (!node.isObject()) {
            throw new IllegalArgumentException("Explorer 响应根节点必须是 JSON 对象");
        }
        return candidate;
    }
}
