package edu.zsc.ai.domain.model.dto.response.ai;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for chat operations.
 *
 * @author zgq
 * @since 0.0.1
 */
@Data
@Builder
public class ChatBlockResponse {

    private Long conversationId;
    private String status;
    private String type;
    private Object data;
    private Long blockId;
    private String toolName;
    private String toolDescription;
}
