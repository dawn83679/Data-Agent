package edu.zsc.ai.api.controller.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import edu.zsc.ai.config.ai.AiModelCatalog;
import edu.zsc.ai.config.ai.AiModelProperties;
import edu.zsc.ai.domain.model.dto.response.ai.ModelOptionResponse;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;

class AiConfigControllerTest {

    private final AiModelProperties modelProperties = new AiModelProperties();
    private final AiModelCatalog aiModelCatalog = new AiModelCatalog(modelProperties);
    private final AiConfigController controller = new AiConfigController(aiModelCatalog);

    AiConfigControllerTest() {
        aiModelCatalog.initialize();
    }

    @Test
    void listModels_returnsConfiguredModelMetadata() {
        ApiResponse<List<ModelOptionResponse>> response = controller.listModels();

        assertNotNull(response);
        assertNotNull(response.getData());

        Map<String, ModelOptionResponse> responsesByModelName = response.getData().stream()
                .collect(Collectors.toMap(ModelOptionResponse::getModelName, Function.identity()));

        assertEquals(aiModelCatalog.listSupportedModels().size(), responsesByModelName.size());
        for (AiModelProperties.ModelDefinition model : aiModelCatalog.listSupportedModels()) {
            ModelOptionResponse option = responsesByModelName.get(model.getModelName());
            assertNotNull(option);
            assertEquals(model.isSupportThinking(), option.isSupportThinking());
            assertEquals(model.getMemoryThreshold(), option.getMemoryThreshold());
            assertEquals(model.getMaxContextTokens(), option.getMaxContextTokens());
        }
    }
}
