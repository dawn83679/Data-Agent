package edu.zsc.ai.domain.service.ai.recall;

import java.util.List;

import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;

public record MemoryRecallPlanningRule(
        String ruleName,
        List<MemoryScopeEnum> scopes,
        MemoryRecallQueryStrategy primaryStrategy,
        MemoryRecallQueryStrategy fallbackStrategy
) {

    public MemoryRecallQueryStrategy strategyFor(MemoryScopeEnum scope) {
        return scope == scopes.get(0) ? primaryStrategy : fallbackStrategy;
    }
}
