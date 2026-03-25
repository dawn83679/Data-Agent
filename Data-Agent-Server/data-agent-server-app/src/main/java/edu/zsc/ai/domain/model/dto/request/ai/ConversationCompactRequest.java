package edu.zsc.ai.domain.model.dto.request.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationCompactRequest {

    @NotBlank(message = "Model cannot be empty")
    private String model;
}
