package edu.zsc.ai.model.dto.request.ai.conversation;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for deleting conversation
 *
 * @author zgq
 */
@Data
public class DeleteConversationRequest {

    /**
     * Conversation ID
     */
    @NotNull(message = "Conversation ID cannot be null")
    private Long id;
}