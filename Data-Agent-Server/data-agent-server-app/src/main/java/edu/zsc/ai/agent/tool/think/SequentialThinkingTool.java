package edu.zsc.ai.agent.tool.think;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.tool.annotation.AgentTool;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.think.model.enums.ThinkingStage;
import edu.zsc.ai.agent.tool.think.model.input.FeedbackInput;
import edu.zsc.ai.agent.tool.think.model.input.ReasoningInput;
import edu.zsc.ai.agent.tool.think.model.input.ThinkingRequest;
import edu.zsc.ai.agent.tool.think.model.output.StructuredReasoning;
import edu.zsc.ai.agent.tool.think.model.output.ThinkingDecision;
import edu.zsc.ai.agent.tool.think.model.output.ThinkingOutput;
import edu.zsc.ai.agent.tool.think.processor.StatePreparation;
import edu.zsc.ai.agent.tool.think.processor.StateSummaryProcessor;
import edu.zsc.ai.agent.tool.think.processor.StructuredReasoningBuilder;
import edu.zsc.ai.agent.tool.think.processor.ThinkingDecisionEngine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@AgentTool
@Slf4j
public class SequentialThinkingTool {

    private final StateSummaryProcessor stateSummaryProcessor = new StateSummaryProcessor();
    private final StructuredReasoningBuilder structuredReasoningBuilder = new StructuredReasoningBuilder();
    private final ThinkingDecisionEngine thinkingDecisionEngine = new ThinkingDecisionEngine();

    @Tool({
            "[GOAL] Produce explicit structured reasoning blocks and next action.",
            "[INPUT] Use Java object ThinkingRequest: {reasoning, feedback, nextThoughtNeeded}.",
            "[OUTPUT] Returns structuredReasoning and decision blocks for rendering and orchestration.",
            "[WRITE-SAFETY] Write workflows should go through SAFETY then askUserConfirm before executeNonSelectSql."
    })
    public AgentToolResult sequentialThinking(
            @P("Thinking request object: reasoning + optional feedback + optional nextThoughtNeeded")
            ThinkingRequest request) {
        ReasoningInput reasoning = request != null ? request.getReasoning() : null;
        FeedbackInput feedback = request != null ? request.getFeedback() : null;
        Boolean nextThoughtNeeded = request != null ? request.getNextThoughtNeeded() : null;
        log.info("[Tool] sequentialThinking, stage={}",
                reasoning != null && reasoning.getMeta() != null ? reasoning.getMeta().getStage() : null);
        try {
            validateReasoning(reasoning);

            ThinkingStage currentStage = reasoning.getMeta().getStage() != null
                    ? reasoning.getMeta().getStage()
                    : ThinkingStage.INTENT;
            boolean continueReasoning = !Boolean.FALSE.equals(nextThoughtNeeded);

            StatePreparation statePreparation = stateSummaryProcessor.prepare(reasoning);
            StructuredReasoning structuredReasoning = structuredReasoningBuilder.build(
                    reasoning,
                    feedback,
                    statePreparation.state(),
                    statePreparation.stateEntries()
            );
            ThinkingDecision decision = thinkingDecisionEngine.buildDecision(
                    currentStage,
                    statePreparation.state(),
                    feedback,
                    continueReasoning
            );

            ThinkingOutput out = new ThinkingOutput();
            out.setStructuredReasoning(structuredReasoning);
            out.setNextStage(decision.getNextStage() != null ? decision.getNextStage().name() : null);
            out.setNextAction(decision.getNextAction());
            out.setActionPayload(decision.getActionPayload());
            out.setCandidatePolicy(decision.getCandidatePolicy());
            out.setSelfCorrection(decision.getSelfCorrection());
            out.setFallbackPolicy(decision.getFallbackPolicy());
            out.setMemoryUpdates(decision.getMemoryUpdates());
            out.setDecisionTrace(decision.getDecisionTrace());
            out.setDecision(decision);
            return AgentToolResult.success(out);
        } catch (Exception e) {
            log.error("[Tool error] sequentialThinking", e);
            return AgentToolResult.fail(e);
        }
    }

    private void validateReasoning(ReasoningInput reasoning) {
        if (reasoning == null) {
            throw new IllegalArgumentException("reasoning must not be null");
        }
        if (reasoning.getMeta() == null) {
            throw new IllegalArgumentException("reasoning.meta must not be null");
        }
        if (StringUtils.isBlank(reasoning.getMeta().getGoal())) {
            throw new IllegalArgumentException("reasoning.meta.goal must not be blank");
        }
    }
}
