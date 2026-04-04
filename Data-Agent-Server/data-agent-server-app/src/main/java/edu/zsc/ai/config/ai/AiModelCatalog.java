package edu.zsc.ai.config.ai;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AiModelProperties.class)
public class AiModelCatalog {

    private final AiModelProperties properties;

    private List<AiModelProperties.ModelDefinition> supportedModels = List.of();
    private Map<String, AiModelProperties.ModelDefinition> modelsByName = Map.of();

    @PostConstruct
    public void initialize() {
        List<AiModelProperties.ModelDefinition> normalized = new ArrayList<>();
        Map<String, AiModelProperties.ModelDefinition> indexed = new LinkedHashMap<>();

        if (properties.getSupported() == null || properties.getSupported().isEmpty()) {
            throw new IllegalStateException("ai.models.supported must not be empty");
        }

        for (AiModelProperties.ModelDefinition source : properties.getSupported()) {
            if (source == null || StringUtils.isBlank(source.getModelName())) {
                throw new IllegalStateException("Each ai.models.supported item must define model-name");
            }
            AiModelProperties.ModelDefinition model = new AiModelProperties.ModelDefinition();
            model.setModelName(source.getModelName().trim());
            model.setApiModelName(StringUtils.defaultIfBlank(source.getApiModelName(), source.getModelName()).trim());
            model.setSupportThinking(source.isSupportThinking());
            model.setMaxContextTokens(source.getMaxContextTokens());
            model.setMemoryThreshold(source.getMemoryThreshold());

            if (indexed.putIfAbsent(model.getModelName(), model) != null) {
                throw new IllegalStateException("Duplicate ai.models.supported model-name: " + model.getModelName());
            }
            normalized.add(model);
        }

        supportedModels = List.copyOf(normalized);
        modelsByName = Collections.unmodifiableMap(indexed);

        requireConfiguredModel(properties.getDefaultModel(), "ai.models.default-model");
        requireConfiguredModel(properties.getCompressionModel(), "ai.models.compression-model");
    }

    public List<AiModelProperties.ModelDefinition> listSupportedModels() {
        return supportedModels;
    }

    public AiModelProperties.ModelDefinition resolve(String requestedModel) {
        String name = StringUtils.isBlank(requestedModel)
                ? properties.getDefaultModel()
                : requestedModel.trim();
        AiModelProperties.ModelDefinition model = modelsByName.get(name);
        if (model == null) {
            throw new IllegalArgumentException("Unknown model: " + name);
        }
        return model;
    }

    public int resolveMemoryThreshold(String modelName) {
        AiModelProperties.ModelDefinition model = modelsByName.get(modelName);
        return model != null ? model.getMemoryThreshold() : defaultModel().getMemoryThreshold();
    }

    public boolean supports(String modelName) {
        return StringUtils.isNotBlank(modelName) && modelsByName.containsKey(modelName.trim());
    }

    public String defaultModelName() {
        return defaultModel().getModelName();
    }

    public String compressionModelName() {
        return requireConfiguredModel(properties.getCompressionModel(), "ai.models.compression-model").getModelName();
    }

    private AiModelProperties.ModelDefinition defaultModel() {
        return requireConfiguredModel(properties.getDefaultModel(), "ai.models.default-model");
    }

    private AiModelProperties.ModelDefinition requireConfiguredModel(String modelName, String propertyName) {
        if (StringUtils.isBlank(modelName)) {
            throw new IllegalStateException(propertyName + " must not be blank");
        }
        AiModelProperties.ModelDefinition model = modelsByName.get(modelName.trim());
        if (model == null) {
            throw new IllegalStateException(propertyName + " references an unsupported model: " + modelName);
        }
        return model;
    }
}
