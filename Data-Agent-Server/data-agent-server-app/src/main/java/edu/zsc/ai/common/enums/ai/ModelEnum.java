package edu.zsc.ai.common.enums.ai;

import lombok.Getter;

@Getter
public enum ModelEnum {

    QWEN3_MAX("qwen3-max", 256000, 230000, false),
    QWEN3_MAX_THINKING("qwen3-max-thinking", 256000, 230000, true),
    QWEN_PLUS("qwen-plus", 1048576, 900000, false);

    private final String modelName;
    private final int maxContextTokens;
    private final int memoryThreshold;
    private final boolean supportThinking;

    ModelEnum(String modelName, int maxContextTokens, int memoryThreshold, boolean supportThinking) {
        this.modelName = modelName;
        this.maxContextTokens = maxContextTokens;
        this.memoryThreshold = memoryThreshold;
        this.supportThinking = supportThinking;
    }

    public static void fromModelName(String modelName) {
        for (ModelEnum limit : values()) {
            if (limit.modelName.equalsIgnoreCase(modelName)) {
                return;
            }
        }
        throw new IllegalArgumentException("Unknown model: " + modelName);
    }
}
