package edu.zsc.ai.domain.model.dto.request.ai.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for saving a message
 *
 * @author zgq
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveMessageRequest {

    /**
     * Conversation ID
     */
    private Long conversationId;

    /**
     * Message content
     */
    private String content;

    /**
     * Message role (user, assistant, system)
     */
    private String role;

    /**
     * Token count (optional, mostly for assistant messages)
     */
    private int tokenCount;

    /**
     * Block type (optional, defaults to TEXT)
     */
    private String blockType;

    /**
     * Extension data (optional)
     */
    private String extensionData;
}
