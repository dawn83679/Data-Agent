package edu.zsc.ai.model.dto.request.ai.conversation;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for getting conversation details
 *
 * @author zgq
 */
@Data
public class GetConversationRequest {

    /**
     * Conversation ID
     */
    @NotNull(message = "Conversation ID cannot be null")
    private Long id;
}