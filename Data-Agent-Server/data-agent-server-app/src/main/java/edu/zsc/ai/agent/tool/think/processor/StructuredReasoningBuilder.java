package edu.zsc.ai.agent.tool.think.processor;

import edu.zsc.ai.agent.tool.think.constant.ThinkingConstants;
import edu.zsc.ai.agent.tool.think.model.input.FeedbackInput;
import edu.zsc.ai.agent.tool.think.model.input.ReasoningContext;
import edu.zsc.ai.agent.tool.think.model.input.ReasoningInput;
import edu.zsc.ai.agent.tool.think.model.input.ReasoningNarrative;
import edu.zsc.ai.agent.tool.think.model.input.ReasoningState;
import edu.zsc.ai.agent.tool.think.model.output.StateEntry;
import edu.zsc.ai.agent.tool.think.model.output.StructuredReasoning;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class StructuredReasoningBuilder {

    public StructuredReasoning build(ReasoningInput reasoning,
                                     FeedbackInput feedback,
                                     ReasoningState state,
                                     List<StateEntry> stateEntries) {
        ReasoningNarrative narrative = reasoning.getNarrative();
        ReasoningContext context = reasoning.getContext();

        String problem = narrative != null ? narrative.getProblem() : null;
        String trigger = narrative != null ? narrative.getTrigger() : null;
        String decompose = narrative != null ? narrative.getDecompose() : null;
        String generate = narrative != null ? narrative.getGenerate() : null;
        String select = narrative != null ? narrative.getSelect() : null;
        String correct = narrative != null ? narrative.getCorrect() : null;
        String memory = narrative != null ? narrative.getMemory() : null;
        String fallback = narrative != null ? narrative.getFallback() : null;
        String stateSummary = context != null ? context.getStateSummary() : null;
        boolean missingStateObject = context == null || context.getState() == null;

        StructuredReasoning structured = new StructuredReasoning();
        structured.setProblem(textOrPlaceholder(problem));
        structured.setTrigger(textOrPlaceholder(trigger));
        structured.setState(state);
        structured.setStateSummary(stateSummary);
        structured.setStateParsed(stateEntries);
        structured.setDecompose(textOrPlaceholder(decompose));
        structured.setGenerate(textOrPlaceholder(generate));
        structured.setSelect(textOrPlaceholder(select));
        structured.setCorrect(textOrPlaceholder(correct));
        structured.setMemory(textOrPlaceholder(memory));
        structured.setFallback(textOrPlaceholder(fallback));
        structured.setFeedback(feedback);

        List<String> missing = new ArrayList<>();
        addMissingIfBlank(missing, ThinkingConstants.DIMENSION_PROBLEM, problem);
        addMissingIfBlank(missing, ThinkingConstants.DIMENSION_TRIGGER, trigger);
        if (missingStateObject && StringUtils.isBlank(stateSummary)) {
            missing.add(ThinkingConstants.DIMENSION_STATE);
        }
        addMissingIfBlank(missing, ThinkingConstants.DIMENSION_DECOMPOSE, decompose);
        addMissingIfBlank(missing, ThinkingConstants.DIMENSION_GENERATE, generate);
        addMissingIfBlank(missing, ThinkingConstants.DIMENSION_SELECT, select);
        addMissingIfBlank(missing, ThinkingConstants.DIMENSION_CORRECT, correct);
        addMissingIfBlank(missing, ThinkingConstants.DIMENSION_MEMORY, memory);
        addMissingIfBlank(missing, ThinkingConstants.DIMENSION_FALLBACK, fallback);
        structured.setMissingDimensions(missing);
        structured.setReadinessScore((ThinkingConstants.READINESS_DIMENSION_COUNT - missing.size())
                / (double) ThinkingConstants.READINESS_DIMENSION_COUNT);
        return structured;
    }

    private String textOrPlaceholder(String value) {
        return StringUtils.isBlank(value) ? ThinkingConstants.PLACEHOLDER_MISSING : value.trim();
    }

    private void addMissingIfBlank(List<String> missing, String name, String value) {
        if (StringUtils.isBlank(value)) {
            missing.add(name);
        }
    }
}
