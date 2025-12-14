package edu.zsc.ai.domain.model.dto.request.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for chat operations.
 *
 * @author zgq
 */
@Data
public class ChatRequest {

    /**
     * Conversation ID.
     * If null, a new conversation will be created.
     */
    private Long conversationId;

    /**
     * User message content.
     */
    @NotBlank(message = "Message cannot be empty")
    private String message;

    /**
     * AI Model to use (optional).
     */
    private String model;
}
