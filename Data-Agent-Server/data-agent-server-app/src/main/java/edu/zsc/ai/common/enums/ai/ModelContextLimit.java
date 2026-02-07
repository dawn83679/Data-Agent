package edu.zsc.ai.common.enums.ai;

public enum ModelContextLimit {

    QWEN3_MAX("qwen3-max", 256000, 230000);

    private final String modelName;
    private final int maxContextTokens;
    private final int memoryThreshold;

    ModelContextLimit(String modelName, int maxContextTokens, int memoryThreshold) {
        this.modelName = modelName;
        this.maxContextTokens = maxContextTokens;
        this.memoryThreshold = memoryThreshold;
    }

    public String getModelName() {
        return modelName;
    }

    public int getMaxContextTokens() {
        return maxContextTokens;
    }

    public int getMemoryThreshold() {
        return memoryThreshold;
    }

    public static ModelContextLimit fromModelName(String modelName) {
        for (ModelContextLimit limit : values()) {
            if (limit.modelName.equalsIgnoreCase(modelName)) {
                return limit;
            }
        }
        throw new IllegalArgumentException("Unknown model: " + modelName);
    }
}
