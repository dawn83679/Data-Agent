package edu.zsc.ai.model.dto.response.ai.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.Message;

/**
 * History message with token count information for AI context
 *
 * @author zgq
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryMessage {

    /**
     * The Spring AI message object
     */
    private Message message;

    /**
     * Token count for this message
     */
    private Integer tokenCount;

    /**
     * Message ID from database
     */
    private Long messageId;

    /**
     * Message role (USER/ASSISTANT)
     */
    private String role;

    /**
     * Message priority (0=normal, 1=summary)
     */
    private Integer priority;

    /**
     * Whether this is a tool result message
     */
    private Boolean isToolResult;

    /**
     * Whether this is a summary message
     */
    private Boolean isSummary;
}