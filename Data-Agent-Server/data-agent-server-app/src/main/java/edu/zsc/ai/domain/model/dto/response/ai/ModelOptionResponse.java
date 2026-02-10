package edu.zsc.ai.domain.model.dto.response.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Option item for model selection (exposed to frontend).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelOptionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Model identifier (e.g. qwen3-max, qwen3-max-thinking).
     */
    private String modelName;

    /**
     * Whether this model supports thinking/reasoning mode (e.g. for displaying brain icon).
     */
    private boolean supportThinking;
}
