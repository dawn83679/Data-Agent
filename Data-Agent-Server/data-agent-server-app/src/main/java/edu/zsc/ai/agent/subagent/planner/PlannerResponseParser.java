package edu.zsc.ai.agent.subagent.planner;

import com.fasterxml.jackson.databind.JsonNode;
import edu.zsc.ai.agent.subagent.contract.SqlPlan;
import edu.zsc.ai.util.JsonUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Parses the Planner model response as a structured SqlPlan.
 * The model is expected to return a single JSON object with summaryText, planSteps, sqlBlocks, and rawResponse.
 */
public final class PlannerResponseParser {

    private PlannerResponseParser() {
    }

    public static SqlPlan parse(String responseText) {
        if (StringUtils.isBlank(responseText)) {
            throw new IllegalArgumentException("Planner response is blank");
        }

        String json = extractJsonObject(responseText);
        SqlPlan plan;
        try {
            plan = JsonUtil.json2Object(json, SqlPlan.class);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Planner response must match the expected JSON schema", e);
        }

        if (StringUtils.isBlank(plan.getSummaryText())
                && CollectionUtils.isEmpty(plan.getPlanSteps())
                && CollectionUtils.isEmpty(plan.getSqlBlocks())
                && StringUtils.isBlank(plan.getRawResponse())) {
            throw new IllegalArgumentException("Planner response JSON does not contain summaryText, planSteps, sqlBlocks, or rawResponse");
        }
        return plan;
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
            throw new IllegalArgumentException("Planner response must contain a JSON object");
        }

        String candidate = trimmed.substring(start, end + 1);
        JsonNode node = JsonUtil.readTree(candidate);
        if (!node.isObject()) {
            throw new IllegalArgumentException("Planner response root must be a JSON object");
        }
        return candidate;
    }
}
