package edu.zsc.ai.agent.tool.think.processor;

import edu.zsc.ai.agent.tool.think.constant.ThinkingConstants;
import edu.zsc.ai.agent.tool.think.model.enums.AmbiguityLevel;
import edu.zsc.ai.agent.tool.think.model.enums.ThinkingStage;
import edu.zsc.ai.agent.tool.think.model.input.FeedbackCorrection;
import edu.zsc.ai.agent.tool.think.model.input.FeedbackInput;
import edu.zsc.ai.agent.tool.think.model.input.FeedbackPreference;
import edu.zsc.ai.agent.tool.think.model.input.FeedbackSafety;
import edu.zsc.ai.agent.tool.think.model.input.FeedbackSelection;
import edu.zsc.ai.agent.tool.think.model.input.ReasoningState;
import edu.zsc.ai.agent.tool.think.model.output.ActionPayload;
import edu.zsc.ai.agent.tool.think.model.output.CandidatePolicy;
import edu.zsc.ai.agent.tool.think.model.output.FallbackPolicy;
import edu.zsc.ai.agent.tool.think.model.output.MemoryUpdate;
import edu.zsc.ai.agent.tool.think.model.output.SelfCorrectionPolicy;
import edu.zsc.ai.agent.tool.think.model.output.ThinkingDecision;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ThinkingDecisionEngine {

    public ThinkingDecision buildDecision(ThinkingStage stage,
                                          ReasoningState state,
                                          FeedbackInput feedback,
                                          boolean continueReasoning) {
        ThinkingDecision decision = new ThinkingDecision();
        List<String> trace = new ArrayList<>();
        trace.add("stage=" + stage.name());
        trace.add("writeOperation=" + bool(state.getWriteOperation()) + ", writeConfirmed=" + isWriteConfirmed(state, feedback));
        trace.add("needUserQuestion=" + bool(state.getNeedUserQuestion()));

        if (!continueReasoning) {
            decision.setNextStage(ThinkingStage.RESPOND);
            decision.setNextAction(ThinkingConstants.ACTION_RESPOND);
            decision.setActionPayload(reasonPayload(ThinkingConstants.REASON_LOOP_TERMINATED));
            decision.setCandidatePolicy(defaultCandidatePolicy(state, feedback, ThinkingConstants.REASON_LOOP_TERMINATED));
            decision.setSelfCorrection(defaultSelfCorrection(state));
            decision.setFallbackPolicy(defaultFallbackPolicy(false, ThinkingConstants.REASON_LOOP_TERMINATED));
            decision.setMemoryUpdates(Collections.emptyList());
            trace.add(ThinkingConstants.TRACE_NEXT_THOUGHT_STOP);
            decision.setDecisionTrace(trace);
            return decision;
        }

        if (bool(state.getNeedUserQuestion())) {
            decision.setNextStage(stage);
            decision.setNextAction(ThinkingConstants.ACTION_ASK_USER_QUESTION);
            decision.setActionPayload(questionPayload(ThinkingConstants.REASON_NEED_USER_CLARIFICATION,
                    ThinkingConstants.QUESTION_TYPE_CLARIFICATION));
            trace.add(ThinkingConstants.TRACE_NEED_USER_QUESTION);
        } else if (stage == ThinkingStage.SAFETY && bool(state.getWriteOperation()) && !isWriteConfirmed(state, feedback)) {
            decision.setNextStage(ThinkingStage.SAFETY);
            decision.setNextAction(ThinkingConstants.ACTION_ASK_USER_CONFIRM);
            decision.setActionPayload(reasonPayload(ThinkingConstants.REASON_WRITE_CONFIRMATION_REQUIRED));
            trace.add(ThinkingConstants.TRACE_WRITE_SAFETY_CONFIRM);
        } else if (stage == ThinkingStage.EXECUTE && bool(state.getHasExecutionError())) {
            decision.setNextStage(ThinkingStage.VERIFY);
            decision.setNextAction(ThinkingConstants.ACTION_CONTINUE_THINKING);
            decision.setActionPayload(instructionPayload(ThinkingConstants.INSTRUCTION_SELF_CORRECT_THEN_RETRY));
            trace.add(ThinkingConstants.TRACE_EXECUTION_ERROR);
        } else if (stage == ThinkingStage.SAFETY && isWriteConfirmed(state, feedback)) {
            decision.setNextStage(ThinkingStage.EXECUTE);
            decision.setNextAction(ThinkingConstants.ACTION_EXECUTE_NON_SELECT_SQL);
            decision.setActionPayload(reasonPayload(ThinkingConstants.REASON_WRITE_CONFIRMED));
            trace.add(ThinkingConstants.TRACE_WRITE_CONFIRMED);
        } else {
            decision.setNextStage(defaultNextStage(stage));
            decision.setNextAction(ThinkingConstants.ACTION_CONTINUE_THINKING);
            decision.setActionPayload(instructionPayload(ThinkingConstants.INSTRUCTION_PROCEED_STAGE_FLOW));
            trace.add(ThinkingConstants.TRACE_DEFAULT_TRANSITION);
        }

        decision.setCandidatePolicy(defaultCandidatePolicy(state, feedback, ThinkingConstants.REASON_DEFAULT_STAGE));
        decision.setSelfCorrection(defaultSelfCorrection(state));
        decision.setFallbackPolicy(defaultFallbackPolicy(false, ThinkingConstants.REASON_DEFAULT_STAGE));
        decision.setMemoryUpdates(buildMemoryUpdates(feedback));
        decision.setDecisionTrace(trace);
        return decision;
    }

    private CandidatePolicy defaultCandidatePolicy(ReasoningState state, FeedbackInput feedback, String reason) {
        CandidatePolicy policy = new CandidatePolicy();
        policy.setMode(ThinkingConstants.MODE_KEEP);
        policy.setReason(reason);
        policy.setCandidateCount(defaultInt(state.getCandidateCount(), ThinkingConstants.DEFAULT_CANDIDATE_COUNT));
        policy.setShortlistMax(state.getAmbiguityLevel() == AmbiguityLevel.HIGH
                ? ThinkingConstants.SHORTLIST_MAX_HIGH
                : ThinkingConstants.SHORTLIST_MAX_DEFAULT);
        policy.setSelectedCandidateId(resolveSelectedCandidateId(state, feedback));
        return policy;
    }

    private SelfCorrectionPolicy defaultSelfCorrection(ReasoningState state) {
        SelfCorrectionPolicy policy = new SelfCorrectionPolicy();
        boolean enabled = bool(state.getHasExecutionError());
        int retriesUsed = defaultInt(state.getRetriesUsed(), ThinkingConstants.DEFAULT_RETRIES_USED);
        int retryBudget = defaultInt(state.getRetryBudget(), ThinkingConstants.DEFAULT_RETRY_BUDGET);
        int remaining = Math.max(0, retryBudget - retriesUsed);
        policy.setEnabled(enabled);
        policy.setTrigger(enabled ? ThinkingConstants.INSTRUCTION_SELF_CORRECT_THEN_RETRY : null);
        policy.setMaxRetries(retryBudget);
        policy.setRetriesUsed(retriesUsed);
        policy.setRemainingRetries(remaining);
        policy.setRetryNow(enabled && remaining > 0);
        return policy;
    }

    private FallbackPolicy defaultFallbackPolicy(boolean abstain, String reason) {
        FallbackPolicy policy = new FallbackPolicy();
        policy.setAbstain(abstain);
        policy.setPath(abstain ? ThinkingConstants.ACTION_ASK_USER_QUESTION : ThinkingConstants.ACTION_CONTINUE_THINKING);
        policy.setReason(reason);
        return policy;
    }

    private List<MemoryUpdate> buildMemoryUpdates(FeedbackInput feedback) {
        if (feedback == null) {
            return Collections.emptyList();
        }
        FeedbackSelection selection = feedback.getSelection();
        FeedbackCorrection correction = feedback.getCorrection();
        FeedbackPreference preference = feedback.getPreference();

        List<MemoryUpdate> updates = new ArrayList<>();
        if (selection != null && StringUtils.isNotBlank(selection.getSelectedCandidateId())) {
            MemoryUpdate item = new MemoryUpdate();
            item.setType(ThinkingConstants.MEMORY_TYPE_SCHEMA_DISAMBIGUATION);
            item.setContent(ThinkingConstants.CONTENT_SELECTED_CANDIDATE_PREFIX + selection.getSelectedCandidateId());
            item.setReason(ThinkingConstants.MEMORY_REASON_SELECTED_CANDIDATE);
            updates.add(item);
        }
        if (correction != null && StringUtils.isNotBlank(correction.getCorrectedSql())) {
            MemoryUpdate item = new MemoryUpdate();
            item.setType(ThinkingConstants.MEMORY_TYPE_WORKLOAD_SQL_HINT);
            item.setContent(correction.getCorrectedSql());
            item.setReason(ThinkingConstants.MEMORY_REASON_CORRECTED_SQL);
            updates.add(item);
        }
        if (preference != null && StringUtils.isNotBlank(preference.getPreferenceHint())) {
            MemoryUpdate item = new MemoryUpdate();
            item.setType(ThinkingConstants.MEMORY_TYPE_QUERY_PREFERENCE);
            item.setContent(preference.getPreferenceHint());
            item.setReason(ThinkingConstants.MEMORY_REASON_PREFERENCE_HINT);
            updates.add(item);
        }
        return updates;
    }

    private ThinkingStage defaultNextStage(ThinkingStage current) {
        return switch (current) {
            case INTENT -> ThinkingStage.SOURCE;
            case SOURCE -> ThinkingStage.SCHEMA;
            case SCHEMA -> ThinkingStage.PLAN;
            case PLAN -> ThinkingStage.GENERATE;
            case GENERATE -> ThinkingStage.SELECT;
            case SELECT -> ThinkingStage.VERIFY;
            case VERIFY -> ThinkingStage.EXECUTE;
            case SAFETY -> ThinkingStage.EXECUTE;
            case EXECUTE, RESPOND -> ThinkingStage.RESPOND;
        };
    }

    private boolean isWriteConfirmed(ReasoningState state, FeedbackInput feedback) {
        if (Boolean.TRUE.equals(state.getWriteConfirmed())) {
            return true;
        }
        FeedbackSafety safety = feedback != null ? feedback.getSafety() : null;
        return safety != null && Boolean.TRUE.equals(safety.getWriteConfirmed());
    }

    private String resolveSelectedCandidateId(ReasoningState state, FeedbackInput feedback) {
        FeedbackSelection selection = feedback != null ? feedback.getSelection() : null;
        if (selection != null && StringUtils.isNotBlank(selection.getSelectedCandidateId())) {
            return selection.getSelectedCandidateId();
        }
        return state.getSelectedCandidateId();
    }

    private boolean bool(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private ActionPayload reasonPayload(String reason) {
        ActionPayload payload = new ActionPayload();
        payload.setReason(reason);
        return payload;
    }

    private ActionPayload instructionPayload(String instruction) {
        ActionPayload payload = new ActionPayload();
        payload.setInstruction(instruction);
        return payload;
    }

    private ActionPayload questionPayload(String reason, String questionType) {
        ActionPayload payload = new ActionPayload();
        payload.setReason(reason);
        payload.setQuestionType(questionType);
        return payload;
    }
}
