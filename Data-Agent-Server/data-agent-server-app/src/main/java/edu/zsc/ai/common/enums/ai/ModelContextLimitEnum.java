package edu.zsc.ai.common.enums.ai;

import lombok.Getter;

@Getter
public enum ModelContextLimitEnum {

    QWEN3_MAX("qwen3-max", 256000, 230000),
    QWEN_PLUS("qwen-plus", 1048576, 900000);

    private final String modelName;
    private final int maxContextTokens;
    private final int memoryThreshold;

    ModelContextLimitEnum(String modelName, int maxContextTokens, int memoryThreshold) {
        this.modelName = modelName;
        this.maxContextTokens = maxContextTokens;
        this.memoryThreshold = memoryThreshold;
    }

    public static ModelContextLimitEnum fromModelName(String modelName) {
        for (ModelContextLimitEnum limit : values()) {
            if (limit.modelName.equalsIgnoreCase(modelName)) {
                return limit;
            }
        }
        throw new IllegalArgumentException("Unknown model: " + modelName);
    }
}
