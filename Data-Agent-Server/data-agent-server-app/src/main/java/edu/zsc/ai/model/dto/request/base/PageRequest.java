package edu.zsc.ai.model.dto.request.base;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic pagination request
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {

    /**
     * Page number, starting from 1
     */
    @Min(value = 1, message = "Current page must be at least 1")
    @Builder.Default
    private Integer current = 1;

    /**
     * Page size
     */
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    @Builder.Default
    private Integer size = 10;
}