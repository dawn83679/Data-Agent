package edu.zsc.ai.api.controller.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

<<<<<<< HEAD
import edu.zsc.ai.config.ai.AiModelCatalog;
import edu.zsc.ai.config.ai.AiModelProperties;
=======
import edu.zsc.ai.common.enums.ai.ModelEnum;
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
import edu.zsc.ai.domain.model.dto.response.ai.ModelOptionResponse;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;

class AiConfigControllerTest {

<<<<<<< HEAD
    private final AiModelProperties modelProperties = new AiModelProperties();
    private final AiModelCatalog aiModelCatalog = new AiModelCatalog(modelProperties);
    private final AiConfigController controller = new AiConfigController(aiModelCatalog);

    AiConfigControllerTest() {
        aiModelCatalog.initialize();
    }

    @Test
    void listModels_returnsConfiguredModelMetadata() {
=======
    private final AiConfigController controller = new AiConfigController();

    @Test
    void listModels_returnsEnumDrivenModelMetadata() {
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
        ApiResponse<List<ModelOptionResponse>> response = controller.listModels();

        assertNotNull(response);
        assertNotNull(response.getData());

        Map<String, ModelOptionResponse> responsesByModelName = response.getData().stream()
                .collect(Collectors.toMap(ModelOptionResponse::getModelName, Function.identity()));

<<<<<<< HEAD
        assertEquals(aiModelCatalog.listSupportedModels().size(), responsesByModelName.size());
        for (AiModelProperties.ModelDefinition model : aiModelCatalog.listSupportedModels()) {
=======
        assertEquals(ModelEnum.values().length, responsesByModelName.size());
        for (ModelEnum model : ModelEnum.values()) {
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
            ModelOptionResponse option = responsesByModelName.get(model.getModelName());
            assertNotNull(option);
            assertEquals(model.isSupportThinking(), option.isSupportThinking());
            assertEquals(model.getMemoryThreshold(), option.getMemoryThreshold());
            assertEquals(model.getMaxContextTokens(), option.getMaxContextTokens());
        }
    }
}
