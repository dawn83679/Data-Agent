package edu.zsc.ai.controller.ai;

import edu.zsc.ai.common.enums.ai.ModelEnum;
import edu.zsc.ai.domain.model.dto.response.ai.ModelOptionResponse;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * AI configuration endpoints (e.g. selectable models for chat).
 */
@RestController
@RequestMapping("/api/ai")
public class AiConfigController {

    /**
     * Returns the list of models available for chat. Frontend uses this for model selector.
     */
    @GetMapping("/models")
    public ApiResponse<List<ModelOptionResponse>> listModels() {
        List<ModelOptionResponse> list = Arrays.stream(ModelEnum.values())
                .map(m -> ModelOptionResponse.builder()
                        .modelName(m.getModelName())
                        .supportThinking(m.isSupportThinking())
                        .build())
                .toList();
        return ApiResponse.success(list);
    }
}
