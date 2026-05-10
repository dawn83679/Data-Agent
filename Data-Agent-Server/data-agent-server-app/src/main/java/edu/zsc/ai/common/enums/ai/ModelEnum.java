package edu.zsc.ai.common.enums.ai;

import lombok.Getter;

@Getter
public enum ModelEnum {

    QWEN3_6_MAX_PREVIEW("qwen3.6-max-preview", 262144, 230000, true),
    QWEN3_MAX_2026_01_23("qwen3-max-2026-01-23", 258048, 230000, true),
    QWEN3_6_PLUS("qwen3.6-plus", 1000000, 900000, false);

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
        String name = (modelName == null || modelName.isBlank()) ? QWEN3_6_MAX_PREVIEW.modelName : modelName.trim();
        return fromModelName(name);
    }
}
