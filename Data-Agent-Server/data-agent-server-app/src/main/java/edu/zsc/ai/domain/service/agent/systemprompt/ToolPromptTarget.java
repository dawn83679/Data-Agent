package edu.zsc.ai.domain.service.agent.systemprompt;

import java.util.Optional;

import edu.zsc.ai.common.enums.ai.PromptEnum;

public enum ToolPromptTarget {

    MAIN_AGENT,
    MAIN_PLAN,
    EXPLORER,
    PLANNER,
    MEMORY_WRITER;

    public static Optional<ToolPromptTarget> fromPrompt(PromptEnum promptEnum) {
        if (promptEnum == null) {
            return Optional.empty();
        }
        return switch (promptEnum) {
            case EN, ZH -> Optional.of(MAIN_AGENT);
            case EN_PLAN, ZH_PLAN -> Optional.of(MAIN_PLAN);
            case EXPLORER -> Optional.of(EXPLORER);
            case PLANNER -> Optional.of(PLANNER);
            case MEMORY_WRITER -> Optional.of(MEMORY_WRITER);
            case COMPRESSION, MEMORY_AUTO_WRITE -> Optional.empty();
        };
    }
}
