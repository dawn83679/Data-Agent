package edu.zsc.ai.agent.tool.think.processor;

import edu.zsc.ai.agent.tool.think.model.input.ReasoningState;
import edu.zsc.ai.agent.tool.think.model.output.StateEntry;

import java.util.List;

public record StatePreparation(
        ReasoningState state,
        List<StateEntry> stateEntries
) {
}

