package edu.zsc.ai.api.controller.ai;

import edu.zsc.ai.config.ai.AiModelCatalog;
import edu.zsc.ai.domain.model.dto.response.ai.ModelOptionResponse;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI configuration endpoints (e.g. selectable models for chat).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiConfigController {

    private final AiModelCatalog aiModelCatalog;

    /**
     * Returns the list of models available for chat. Frontend uses this for model selector.
     */
    @GetMapping("/models")
    public ApiResponse<List<ModelOptionResponse>> listModels() {
        List<ModelOptionResponse> list = aiModelCatalog.listSupportedModels().stream()
                .map(m -> ModelOptionResponse.builder()
                        .modelName(m.getModelName())
                        .supportThinking(m.isSupportThinking())
                        .memoryThreshold(m.getMemoryThreshold())
                        .maxContextTokens(m.getMaxContextTokens())
                        .build())
                .toList();
        return ApiResponse.success(list);
    }
}
