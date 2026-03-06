package edu.zsc.ai.agent.tool.think.processor;

import edu.zsc.ai.agent.tool.think.constant.ThinkingConstants;
import edu.zsc.ai.agent.tool.think.model.enums.AmbiguityLevel;
import edu.zsc.ai.agent.tool.think.model.input.CandidateInput;
import edu.zsc.ai.agent.tool.think.model.input.ReasoningContext;
import edu.zsc.ai.agent.tool.think.model.input.ReasoningInput;
import edu.zsc.ai.agent.tool.think.model.input.ReasoningNarrative;
import edu.zsc.ai.agent.tool.think.model.input.ReasoningState;
import edu.zsc.ai.agent.tool.think.model.output.StateEntry;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StateSummaryProcessor {

    public StatePreparation prepare(ReasoningInput reasoning) {
        ReasoningContext context = reasoning.getContext();
        ReasoningNarrative narrative = reasoning.getNarrative();
        ReasoningState state = context != null && context.getState() != null ? context.getState() : new ReasoningState();
        String stateSummary = context != null ? context.getStateSummary() : null;
        List<CandidateInput> candidates = context != null ? context.getCandidates() : null;
        String trigger = narrative != null ? narrative.getTrigger() : null;

        List<StateEntry> entries = parseStateSummaryEntries(stateSummary);
        applyStateEntries(state, entries);
        normalizeDerivedState(state, candidates, trigger);
        return new StatePreparation(state, entries);
    }

    private void normalizeDerivedState(ReasoningState state, List<CandidateInput> candidates, String trigger) {
        if (state.getCandidateCount() == null && candidates != null) {
            state.setCandidateCount(candidates.size());
        }
        if (state.getCandidateCount() == null) {
            state.setCandidateCount(inferCandidateCountFromTrigger(trigger));
        }
        if (state.getRetryBudget() == null) {
            state.setRetryBudget(ThinkingConstants.DEFAULT_RETRY_BUDGET);
        }
        if (state.getRetriesUsed() == null) {
            state.setRetriesUsed(ThinkingConstants.DEFAULT_RETRIES_USED);
        }
        if (state.getAmbiguityLevel() == null) {
            state.setAmbiguityLevel(inferAmbiguityLevel(trigger));
        }
        if (state.getConfidence() == null) {
            state.setConfidence(ThinkingConstants.DEFAULT_CONFIDENCE);
        }
    }

    private List<StateEntry> parseStateSummaryEntries(String stateSummary) {
        if (StringUtils.isBlank(stateSummary)) {
            return new ArrayList<>();
        }
        List<StateEntry> entries = new ArrayList<>();
        String normalized = stateSummary.replace(ThinkingConstants.SYMBOL_NEWLINE, ThinkingConstants.SYMBOL_SEMICOLON);
        String[] parts = normalized.split(ThinkingConstants.SYMBOL_SEMICOLON);
        for (String part : parts) {
            if (StringUtils.isBlank(part)) {
                continue;
            }
            String segment = part.trim();
            int idx = segment.indexOf(ThinkingConstants.SYMBOL_EQUAL);
            if (idx < 0) {
                idx = segment.indexOf(ThinkingConstants.SYMBOL_COLON);
            }
            if (idx < 0) {
                continue;
            }
            String key = StringUtils.trimToEmpty(segment.substring(0, idx));
            String value = StringUtils.trimToEmpty(segment.substring(idx + 1));
            if (StringUtils.isBlank(key)) {
                continue;
            }
            StateEntry entry = new StateEntry();
            entry.setKey(key);
            entry.setValue(value);
            entries.add(entry);
        }
        return entries;
    }

    private void applyStateEntries(ReasoningState state, List<StateEntry> entries) {
        for (StateEntry entry : entries) {
            if (entry == null || StringUtils.isBlank(entry.getKey())) {
                continue;
            }
            String key = normalizeStateKey(entry.getKey());
            String value = entry.getValue();
            switch (key) {
                case ThinkingConstants.STATE_KEY_SOURCE_RESOLVED ->
                        state.setSourceResolved(parseBoolean(value, state.getSourceResolved()));
                case ThinkingConstants.STATE_KEY_SCHEMA_READY ->
                        state.setSchemaReady(parseBoolean(value, state.getSchemaReady()));
                case ThinkingConstants.STATE_KEY_WRITE_OPERATION ->
                        state.setWriteOperation(parseBoolean(value, state.getWriteOperation()));
                case ThinkingConstants.STATE_KEY_WRITE_CONFIRMED ->
                        state.setWriteConfirmed(parseBoolean(value, state.getWriteConfirmed()));
                case ThinkingConstants.STATE_KEY_NEED_USER_QUESTION ->
                        state.setNeedUserQuestion(parseBoolean(value, state.getNeedUserQuestion()));
                case ThinkingConstants.STATE_KEY_HAS_EXECUTION_ERROR ->
                        state.setHasExecutionError(parseBoolean(value, state.getHasExecutionError()));
                case ThinkingConstants.STATE_KEY_CONFIDENCE ->
                        state.setConfidence(parseDouble(value, state.getConfidence()));
                case ThinkingConstants.STATE_KEY_RETRIES_USED ->
                        state.setRetriesUsed(parseInteger(value, state.getRetriesUsed()));
                case ThinkingConstants.STATE_KEY_RETRY_BUDGET ->
                        state.setRetryBudget(parseInteger(value, state.getRetryBudget()));
                case ThinkingConstants.STATE_KEY_CANDIDATE_COUNT ->
                        state.setCandidateCount(parseInteger(value, state.getCandidateCount()));
                case ThinkingConstants.STATE_KEY_SELECTED_CANDIDATE_ID ->
                        state.setSelectedCandidateId(StringUtils.trimToNull(value));
                case ThinkingConstants.STATE_KEY_AMBIGUITY_LEVEL ->
                        state.setAmbiguityLevel(parseAmbiguityLevel(value, state.getAmbiguityLevel()));
                default -> {
                    // ignore unknown keys
                }
            }
        }
    }

    private String normalizeStateKey(String rawKey) {
        String key = rawKey.trim().toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
        return switch (key) {
            case "sourceresolved" -> ThinkingConstants.STATE_KEY_SOURCE_RESOLVED;
            case "schemaready" -> ThinkingConstants.STATE_KEY_SCHEMA_READY;
            case "writeoperation" -> ThinkingConstants.STATE_KEY_WRITE_OPERATION;
            case "writeconfirmed" -> ThinkingConstants.STATE_KEY_WRITE_CONFIRMED;
            case "needuserquestion" -> ThinkingConstants.STATE_KEY_NEED_USER_QUESTION;
            case "hasexecutionerror", "executionerror" -> ThinkingConstants.STATE_KEY_HAS_EXECUTION_ERROR;
            case "confidence" -> ThinkingConstants.STATE_KEY_CONFIDENCE;
            case "retriesused" -> ThinkingConstants.STATE_KEY_RETRIES_USED;
            case "retrybudget" -> ThinkingConstants.STATE_KEY_RETRY_BUDGET;
            case "candidatecount" -> ThinkingConstants.STATE_KEY_CANDIDATE_COUNT;
            case "selectedcandidateid" -> ThinkingConstants.STATE_KEY_SELECTED_CANDIDATE_ID;
            case "ambiguitylevel", "ambiguity" -> ThinkingConstants.STATE_KEY_AMBIGUITY_LEVEL;
            default -> rawKey;
        };
    }

    private AmbiguityLevel inferAmbiguityLevel(String trigger) {
        String triggerText = StringUtils.defaultString(trigger).toLowerCase(Locale.ROOT);
        if (triggerText.contains(ThinkingConstants.TRIGGER_HINT_AMBIGU)
                || triggerText.contains(ThinkingConstants.TRIGGER_HINT_CN_SAME_NAME)
                || triggerText.contains(ThinkingConstants.TRIGGER_HINT_CN_CONFLICT)) {
            return AmbiguityLevel.HIGH;
        }
        return AmbiguityLevel.MEDIUM;
    }

    private int inferCandidateCountFromTrigger(String trigger) {
        String text = StringUtils.defaultString(trigger).toLowerCase(Locale.ROOT);
        if (text.contains(ThinkingConstants.TRIGGER_HINT_MULTI)
                || text.contains(ThinkingConstants.TRIGGER_HINT_MULTI_CN)) {
            return ThinkingConstants.SHORTLIST_MAX_HIGH;
        }
        if (text.contains(ThinkingConstants.TRIGGER_HINT_SINGLE)
                || text.contains(ThinkingConstants.TRIGGER_HINT_SINGLE_CN)) {
            return 1;
        }
        return ThinkingConstants.DEFAULT_CANDIDATE_COUNT;
    }

    private Boolean parseBoolean(String value, Boolean fallback) {
        if (StringUtils.isBlank(value)) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return "true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized);
    }

    private Integer parseInteger(String value, Integer fallback) {
        if (StringUtils.isBlank(value)) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Double parseDouble(String value, Double fallback) {
        if (StringUtils.isBlank(value)) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private AmbiguityLevel parseAmbiguityLevel(String value, AmbiguityLevel fallback) {
        if (StringUtils.isBlank(value)) {
            return fallback;
        }
        try {
            return AmbiguityLevel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
