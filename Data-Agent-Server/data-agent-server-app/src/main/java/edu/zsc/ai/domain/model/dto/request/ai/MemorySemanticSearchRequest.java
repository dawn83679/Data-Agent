package edu.zsc.ai.domain.model.dto.request.ai;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MemorySemanticSearchRequest {

    @NotBlank(message = "queryText is required")
    private String queryText;

    @Min(value = 1, message = "limit must be at least 1")
    @Max(value = 30, message = "limit must not exceed 30")
    private Integer limit = 10;

    @DecimalMin(value = "0.0", message = "minScore must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "minScore must be between 0 and 1")
    private Double minScore = 0.72;
}
