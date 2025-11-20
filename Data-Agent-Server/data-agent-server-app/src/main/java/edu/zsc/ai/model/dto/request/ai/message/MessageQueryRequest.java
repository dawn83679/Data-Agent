package edu.zsc.ai.model.dto.request.ai.message;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message query request for pagination
 *
 * @author zgq
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageQueryRequest {

    /**
     * Conversation ID
     */
    @NotNull(message = "Conversation ID cannot be null")
    private Long conversationId;

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

    /**
     * Cursor-based pagination - message ID for cursor position
     * Used for stable ordering and better performance
     */
    private Long cursorId;


}