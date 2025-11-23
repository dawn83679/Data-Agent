package edu.zsc.ai.enums.ai;

import org.apache.commons.lang3.StringUtils;

/**
 * Model configuration enum with token limits and compression thresholds.
 *
 * @author zgq
 */
public enum ModelEnum {

    /**
     * Qwen3 Coder Plus model
     * Max input: 1M tokens (1000k)
     * Max output: 64k tokens
     * Compression threshold: 800k tokens
     */
    QWEN3_CODER_PLUS("qwen3-coder-plus", 1000, 64, 800);

    private final String modelName;
    private final Integer maxInputTokens; // in k tokens
    private final Integer maxOutputTokens; // in k tokens
    private final Integer compressionThreshold; // in k tokens

    ModelEnum(String modelName, Integer maxInputTokens, Integer maxOutputTokens, Integer compressionThreshold) {
        this.modelName = modelName;
        this.maxInputTokens = maxInputTokens;
        this.maxOutputTokens = maxOutputTokens;
        this.compressionThreshold = compressionThreshold;
    }

    public String getModelName() {
        return modelName;
    }

    public Integer getMaxInputTokens() {
        return maxInputTokens;
    }

    public Integer getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public Integer getCompressionThreshold() {
        return compressionThreshold;
    }

    /**
     * Get max input tokens in actual token count (multiply by 1000).
     */
    public Integer getMaxInputTokenCount() {
        return maxInputTokens * 1000;
    }

    /**
     * Get max output tokens in actual token count (multiply by 1000).
     */
    public Integer getMaxOutputTokenCount() {
        return maxOutputTokens * 1000;
    }

    /**
     * Get compression threshold in actual token count (multiply by 1000).
     */
    public Integer getCompressionThresholdTokenCount() {
        return compressionThreshold * 1000;
    }

    /**
     */
    public static ModelEnum findByModelNameOrDefaultModel(String modelName) {
        if (StringUtils.isBlank(modelName)) {
            return  getDefault();
        }
        for (ModelEnum model : values()) {
            if (model.getModelName().equals(modelName)) {
                return model;
            }
        }
        return  getDefault();
    }

    /**
     * Get default model.
     */
    public static ModelEnum getDefault() {
        return QWEN3_CODER_PLUS;
    }
}