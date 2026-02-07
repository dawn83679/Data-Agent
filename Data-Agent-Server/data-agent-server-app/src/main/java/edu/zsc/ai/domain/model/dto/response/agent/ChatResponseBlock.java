package edu.zsc.ai.domain.model.dto.response.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chat Response Block DTO
 * Represents different types of content in the streaming response
 *
 * @author Data-Agent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseBlock {

    /**
     * Type of response block
     */
    private String type;

    /**
     * Text content (for TEXT and THOUGHT types)
     */
    private String content;

    /**
     * Tool name (for TOOL_CALL type)
     */
    private String toolName;

    /**
     * Tool arguments (for TOOL_CALL type)
     */
    private String toolArguments;

    /**
     * Tool result (for TOOL_RESULT type)
     */
    private String toolResult;

    /**
     * Whether this is the final block
     */
    private boolean done;

    /**
     * Create a text block
     */
    public static ChatResponseBlock text(String content) {
        return ChatResponseBlock.builder()
                .type(ChatResponseBlockType.TEXT.name())
                .content(content)
                .done(false)
                .build();
    }

    /**
     * Create a thought block
     */
    public static ChatResponseBlock thought(String content) {
        return ChatResponseBlock.builder()
                .type(ChatResponseBlockType.THOUGHT.name())
                .content(content)
                .done(false)
                .build();
    }

    /**
     * Create a tool call block
     */
    public static ChatResponseBlock toolCall(String toolName, String arguments) {
        return ChatResponseBlock.builder()
                .type(ChatResponseBlockType.TOOL_CALL.name())
                .toolName(toolName)
                .toolArguments(arguments)
                .done(false)
                .build();
    }

    /**
     * Create a tool result block
     */
    public static ChatResponseBlock toolResult(String result) {
        return ChatResponseBlock.builder()
                .type(ChatResponseBlockType.TOOL_RESULT.name())
                .toolResult(result)
                .done(false)
                .build();
    }

    /**
     * Create a done block
     */
    public static ChatResponseBlock done() {
        return ChatResponseBlock.builder()
                .type(ChatResponseBlockType.DONE.name())
                .done(true)
                .build();
    }
}
