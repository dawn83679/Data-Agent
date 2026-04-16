package edu.zsc.ai.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "ai.models")
public class AiModelProperties {

    private String defaultModel = "qwen3.5-plus";

    private String compressionModel = "qwen3.5-plus";

    private List<ModelDefinition> supported = defaultSupportedModels();

    @Data
    public static class ModelDefinition {

        private String modelName;

        /**
         * Provider-side model identifier. Defaults to modelName when omitted.
         */
        private String apiModelName;

        private boolean supportThinking;

        private int maxContextTokens;

        private int memoryThreshold;
    }

    private static List<ModelDefinition> defaultSupportedModels() {
        List<ModelDefinition> models = new ArrayList<>();
        models.add(model("qwen3.5-plus", "qwen3.5-plus", false, 1_048_576, 900_000));
        models.add(model("qwen3.6-plus", "qwen3.6-plus", false, 1_000_000, 900_000));
        models.add(model("qwen3-max-2026-01-23", "qwen3-max-2026-01-23", false, 256_000, 230_000));
        models.add(model("qwen3-max-thinking", "qwen3-max-2026-01-23", true, 256_000, 230_000));
        return models;
    }

    private static ModelDefinition model(String modelName,
                                         String apiModelName,
                                         boolean supportThinking,
                                         int maxContextTokens,
                                         int memoryThreshold) {
        ModelDefinition model = new ModelDefinition();
        model.setModelName(modelName);
        model.setApiModelName(apiModelName);
        model.setSupportThinking(supportThinking);
        model.setMaxContextTokens(maxContextTokens);
        model.setMemoryThreshold(memoryThreshold);
        return model;
    }
}
