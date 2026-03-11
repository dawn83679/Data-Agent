package edu.zsc.ai.common.enums.ai;

import lombok.Getter;

@Getter
public enum ModelEnum {

    QWEN3_MAX("qwen3-max", 256000, 230000, false),
    QWEN3_MAX_THINKING("qwen3-max-thinking", 256000, 230000, true),
    QWEN_PLUS("qwen-plus", 1048576, 900000, false),
    GLM_5("glm-5", 200000, 128000, true),
    MINIMAX_M2_5("MiniMax-M2.5", 204800, 150000, true);

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

    /**
     * Resolves a model name to a valid ModelEnum, falling back to QWEN3_MAX if blank.
     *
     * @throws IllegalArgumentException if the model name is not supported
     */
    public static ModelEnum resolve(String modelName) {
        String name = (modelName == null || modelName.isBlank()) ? QWEN3_MAX.modelName : modelName.trim();
        return fromModelName(name);
    }
}
