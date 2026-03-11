package edu.zsc.ai.agent.tool.think;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.think.model.input.CandidateObject;
import edu.zsc.ai.agent.tool.think.model.input.ThinkingRequest;
import edu.zsc.ai.agent.tool.think.model.output.ChecklistItem;
import edu.zsc.ai.agent.tool.think.model.output.PreflightChecklist;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AgentTool
@Slf4j
public class ThinkingTool {

    @Tool({
            "Calling this tool significantly improves task accuracy and reduces wrong-target or skip-step ",
            "errors; experts spend most of their time thinking before acting. ",
            "Reasoning engine: understand the goal, break down tasks, get checklist (SURVEY → DISAMBIGUATE → EXPLORE) and next-step advice.",
            "",
            "When to Use: at the start of new requests; before write operations; when results are surprising or you are unsure what to do next.",
            "When NOT to Use: for trivial single-step queries when target is already clear and no write is involved.",
            "Relation: call first when task is non-trivial; follow the returned toolToCall and checklist; then proceed with getEnvironmentOverview/searchObjects/getObjectDetail or enterPlanMode as recommended."
    })
    public AgentToolResult thinking(
            @P("Thinking request: goal, analysis, isWrite flag, and candidates list (objects discovered so far)")
            ThinkingRequest request) {
        log.info("[Tool] thinking, goal={}, isWrite={}, candidates={}",
                request != null ? request.getGoal() : null,
                request != null && request.isWrite(),
                request != null && request.getCandidates() != null ? request.getCandidates().size() : 0);
        try {
            validate(request);
            PreflightChecklist checklist = buildChecklist(request);
            return AgentToolResult.success(checklist);
        } catch (Exception e) {
            log.error("[Tool error] thinking", e);
            return AgentToolResult.fail("thinking failed: " + e.getMessage()
                    + ". Ensure goal and analysis fields are provided and not blank.");
        }
    }

    private void validate(ThinkingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("ThinkingRequest must not be null");
        }
        if (StringUtils.isBlank(request.getGoal())) {
            throw new IllegalArgumentException("goal must not be blank");
        }
        if (StringUtils.isBlank(request.getAnalysis())) {
            throw new IllegalArgumentException("analysis must not be blank");
        }
    }

    private PreflightChecklist buildChecklist(ThinkingRequest request) {
        List<ChecklistItem> required = new ArrayList<>();
        List<ChecklistItem> recommended = new ArrayList<>();
        List<String> risks = new ArrayList<>();

        List<CandidateObject> candidates = request.getCandidates();
        boolean noCandidates = candidates == null || candidates.isEmpty();
        int candidateCount = noCandidates ? 0 : candidates.size();

        // Determine phase based on candidate list state
        if (noCandidates) {
            // Phase: SURVEY — nothing discovered yet
            required.add(ChecklistItem.builder()
                    .action("Survey data landscape — scan ALL connections and databases, "
                            + "collect candidate tables, then confirm target with user if ambiguous")
                    .toolToCall("getEnvironmentOverview then searchObjects to collect candidate tables")
                    .reason("No candidates identified yet. You MUST scan ALL connections and databases "
                            + "to build a complete candidate set before narrowing down. "
                            + "Do NOT deep-dive on the first match — stopping at the first match causes wrong-database/wrong-table operations. Only after full survey, use askUserQuestion if 2+ candidates.")
                    .build());
        } else if (candidateCount > 1) {
            // Phase: DISAMBIGUATE — multiple candidates, need user to choose
            String candidateList = candidates.stream()
                    .map(this::formatCandidate)
                    .collect(Collectors.joining(", "));
            required.add(ChecklistItem.builder()
                    .action("Disambiguate — " + candidateCount + " candidates found, ask user to choose")
                    .toolToCall("askUserQuestion")
                    .reason("Multiple candidates discovered: [" + candidateList + "]. "
                            + "Do NOT silently pick one — ask the user which target to use.")
                    .build());
        } else {
            // Phase: EXPLORE — single confirmed target, deep-dive
            CandidateObject target = candidates.get(0);
            required.add(ChecklistItem.builder()
                    .action("Explore confirmed target: " + formatCandidate(target))
                    .toolToCall("getObjectDetail")
                    .reason("Target confirmed — retrieve structure (columns, types, constraints) "
                            + "before generating SQL.")
                    .build());
        }

        // Write operation checks
        if (request.isWrite()) {
            risks.add("Write operation detected — must call askUserConfirm before executeNonSelectSql.");

            if (!noCandidates) {
                recommended.add(ChecklistItem.builder()
                        .action("Verify write impact: check row counts and indexes on target tables")
                        .toolToCall("getObjectDetail (returns row count and indexes)")
                        .reason("Write operation — understand impact scope before proceeding")
                        .build());
            }
        }

        // Risk detection from analysis text
        String analysis = request.getAnalysis().toLowerCase();
        if (analysis.contains("large table") || analysis.contains("大表")) {
            risks.add("Large table referenced — include WHERE/LIMIT to prevent full-table scans.");
        }
        if (analysis.contains("pii") || analysis.contains("sensitive") || analysis.contains("敏感")) {
            risks.add("Sensitive data — apply minimal disclosure.");
        }
        if (analysis.contains("ambiguous") || analysis.contains("歧义") || analysis.contains("conflict")) {
            risks.add("Ambiguity detected — consider askUserQuestion to clarify.");
        }

        // Complexity assessment
        int complexityScore = (noCandidates ? 2 : 0)
                + (candidateCount > 1 ? 1 : 0)
                + (request.isWrite() ? 2 : 0)
                + (candidateCount > 2 ? 2 : 0);
        String complexity;
        if (complexityScore >= 5) {
            complexity = "complex";
        } else if (complexityScore >= 3) {
            complexity = "moderate";
        } else {
            complexity = "simple";
        }

        boolean suggestPlan = complexityScore >= 5 || candidateCount >= 4;
        String planReason = null;
        if (suggestPlan) {
            List<String> reasons = new ArrayList<>();
            if (complexityScore >= 5) reasons.add("high complexity score (" + complexityScore + ")");
            if (candidateCount >= 4) reasons.add(candidateCount + " candidate objects");
            if (request.isWrite() && candidateCount > 1) reasons.add("multi-table write operation");
            planReason = "Recommend Plan mode: " + String.join(", ", reasons);
        }

        // Build summary
        StringBuilder summary = new StringBuilder();
        summary.append("Goal: ").append(request.getGoal());
        if (request.isWrite()) {
            summary.append(" [WRITE OPERATION]");
        }
        if (noCandidates) {
            summary.append(" | Phase: SURVEY — map all data sources before selecting target.");
        } else if (candidateCount > 1) {
            summary.append(" | Phase: DISAMBIGUATE — ").append(candidateCount)
                    .append(" candidates found, ask user to choose.");
        } else {
            summary.append(" | Phase: EXPLORE — deep-dive into confirmed target.");
        }

        return PreflightChecklist.builder()
                .summary(summary.toString())
                .requiredBefore(required)
                .recommended(recommended)
                .risks(risks)
                .complexityAssessment(complexity)
                .suggestPlanMode(suggestPlan)
                .suggestPlanModeReason(planReason)
                .build();
    }

    private String formatCandidate(CandidateObject c) {
        StringBuilder sb = new StringBuilder();
        if (c.getDatabaseName() != null) {
            sb.append(c.getDatabaseName()).append(".");
        }
        if (c.getSchemaName() != null) {
            sb.append(c.getSchemaName()).append(".");
        }
        sb.append(c.getObjectName() != null ? c.getObjectName() : "?");
        if (c.getConnectionId() != null) {
            sb.append("(conn:").append(c.getConnectionId()).append(")");
        }
        return sb.toString();
    }
}
