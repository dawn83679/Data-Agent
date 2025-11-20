package edu.zsc.ai.model.dto.response.ai.conversation;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for conversation
 *
 * @author zgq
 */
@Data
public class ConversationResponse {

    /**
     * Primary key ID for conversation
     */
    private Long id;

    /**
     * Associated user ID
     */
    private Long userId;

    /**
     * Conversation title
     */
    private String title;

    /**
     * Token usage statistics
     */
    private Integer tokenCount;

    /**
     * Created time
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Updated time
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}