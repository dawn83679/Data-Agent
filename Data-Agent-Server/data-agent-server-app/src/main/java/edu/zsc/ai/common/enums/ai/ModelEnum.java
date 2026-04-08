package edu.zsc.ai.common.enums.ai;

import lombok.Getter;

@Getter
public enum ModelEnum {

    QWEN3_5_PLUS("qwen3.5-plus", 1048576, 900000, false),
    QWEN3_MAX("qwen3-max-2026-01-23", 256000, 230000, false),
<<<<<<< HEAD
    QWEN3_MAX_THINKING("qwen3-max-thinking", 256000, 230000, true);
=======
    QWEN3_MAX_THINKING("qwen3-max-thinking", 256000, 230000, true),
    QWEN_PLUS("qwen-plus", 1048576, 900000, false);
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793

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

    public static ModelEnum fromModelName(String modelName) {
        for (ModelEnum limit : values()) {
            if (limit.modelName.equalsIgnoreCase(modelName)) {
                return limit;
            }
        }
        throw new IllegalArgumentException("Unknown model: " + modelName);
    }

    public static ModelEnum resolve(String modelName) {
        String name = (modelName == null || modelName.isBlank()) ? QWEN3_5_PLUS.modelName : modelName.trim();
        return fromModelName(name);
    }
}
