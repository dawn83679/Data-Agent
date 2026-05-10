package edu.zsc.ai.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "ai.models")
public class AiModelProperties {

    private String defaultModel = "qwen3.6-max-preview";

    private String compressionModel = "qwen3.6-plus";

    private List<ModelDefinition> supported = defaultSupportedModels();

    @Data
    public static class ModelDefinition {

        private String modelName;

        /**
         * Provider-side model identifier. Defaults to modelName when omitted.
         */
        private String apiModelName;

        private boolean supportThinking;

        private boolean chatVisible = true;

        private int maxContextTokens;

        private int memoryThreshold;
    }

    private static List<ModelDefinition> defaultSupportedModels() {
        List<ModelDefinition> models = new ArrayList<>();
        models.add(model("qwen3.6-max-preview", "qwen3.6-max-preview", true, true, 262_144, 230_000));
        models.add(model("qwen3-max-2026-01-23", "qwen3-max-2026-01-23", true, true, 258_048, 230_000));
        models.add(model("qwen3.6-plus", "qwen3.6-plus", false, false, 1_000_000, 900_000));
        return models;
    }

    private static ModelDefinition model(String modelName,
                                         String apiModelName,
                                         boolean supportThinking,
                                         boolean chatVisible,
                                         int maxContextTokens,
                                         int memoryThreshold) {
        ModelDefinition model = new ModelDefinition();
        model.setModelName(modelName);
        model.setApiModelName(apiModelName);
        model.setSupportThinking(supportThinking);
        model.setChatVisible(chatVisible);
        model.setMaxContextTokens(maxContextTokens);
        model.setMemoryThreshold(memoryThreshold);
        return model;
    }
}
