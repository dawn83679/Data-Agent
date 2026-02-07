package edu.zsc.ai.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chat Request DTO
 */
@Data
@NoArgsConstructor
public class ChatRequest extends BaseRequest {

    @NotBlank(message = "Message cannot be empty")
    private String message;
}
