package edu.zsc.ai.api.controller.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import edu.zsc.ai.common.enums.ai.ModelEnum;
import edu.zsc.ai.domain.model.dto.response.ai.ModelOptionResponse;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;

class AiConfigControllerTest {

    private final AiConfigController controller = new AiConfigController();

    @Test
    void listModels_returnsEnumDrivenModelMetadata() {
        ApiResponse<List<ModelOptionResponse>> response = controller.listModels();

        assertNotNull(response);
        assertNotNull(response.getData());

        Map<String, ModelOptionResponse> responsesByModelName = response.getData().stream()
                .collect(Collectors.toMap(ModelOptionResponse::getModelName, Function.identity()));

        assertEquals(ModelEnum.values().length, responsesByModelName.size());
        for (ModelEnum model : ModelEnum.values()) {
            ModelOptionResponse option = responsesByModelName.get(model.getModelName());
            assertNotNull(option);
            assertEquals(model.isSupportThinking(), option.isSupportThinking());
            assertEquals(model.getMemoryThreshold(), option.getMemoryThreshold());
            assertEquals(model.getMaxContextTokens(), option.getMaxContextTokens());
        }
    }
}
