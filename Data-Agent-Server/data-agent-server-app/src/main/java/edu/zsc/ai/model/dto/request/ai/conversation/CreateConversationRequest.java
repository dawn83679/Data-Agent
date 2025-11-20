package edu.zsc.ai.model.dto.request.ai.conversation;

import lombok.Data;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating conversation
 *
 * @author zgq
 */
@Data
public class CreateConversationRequest {

    /**
     * Conversation title, can be empty and will be generated from first message
     */
    @Size(max = 255, message = "Title length cannot exceed 255 characters")
    private String title;
}