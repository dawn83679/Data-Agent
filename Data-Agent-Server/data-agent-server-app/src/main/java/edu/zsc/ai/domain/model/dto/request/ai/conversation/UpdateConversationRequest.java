package edu.zsc.ai.domain.model.dto.request.ai.conversation;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating conversation
 *
 * @author zgq
 */
@Data
public class UpdateConversationRequest {

    /**
     * Conversation ID
     */
    @NotNull(message = "Conversation ID cannot be null")
    private Long id;

    /**
     * Conversation title to update
     */
    @NotBlank(message = "Conversation title cannot be blank")
    @Size(max = 255, message = "Title length cannot exceed 255 characters")
    private String title;
}