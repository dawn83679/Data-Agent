package edu.zsc.ai.domain.model.dto.response.agent;

import edu.zsc.ai.common.enums.ai.MessageBlockEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseBlock {
    private String type;
    private String content;
    private String toolName;
    private String toolArguments;
    private String toolResult;
    private boolean done;

    public static ChatResponseBlock text(String content) {
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.TEXT.name())
                .content(content)
                .done(false)
                .build();
    }

    public static ChatResponseBlock thought(String content) {
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.THOUGHT.name())
                .content(content)
                .done(false)
                .build();
    }

    public static ChatResponseBlock toolCall(String toolName, String arguments) {
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.TOOL_CALL.name())
                .toolName(toolName)
                .toolArguments(arguments)
                .done(false)
                .build();
    }

    public static ChatResponseBlock toolResult(String result) {
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.TOOL_RESULT.name())
                .toolResult(result)
                .done(false)
                .build();
    }

}
