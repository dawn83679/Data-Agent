package edu.zsc.ai.domain.model.dto.response.agent;

import edu.zsc.ai.common.enums.ai.MessageBlockEnum;
import edu.zsc.ai.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseBlock {
    private String type;
    private String data;
    private Long conversationId;
    private boolean done;

    public static ChatResponseBlock text(String data) {
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.TEXT.name())
                .data(data)
                .done(false)
                .build();
    }

    public static ChatResponseBlock thought(String data) {
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.THOUGHT.name())
                .data(data)
                .done(false)
                .build();
    }

    /**
     * End-of-stream block: done=true and optional conversationId for new sessions.
     */
    public static ChatResponseBlock doneBlock(Long conversationId) {
        return ChatResponseBlock.builder()
                .done(true)
                .conversationId(conversationId)
                .build();
    }

    /**
     * Tool call block: data is JSON {"toolName":"...", "arguments":"..."}.
     */
    public static ChatResponseBlock toolCall(String toolName, String arguments) {
        String data = JsonUtil.object2json(Map.of(
                "toolName", toolName != null ? toolName : "",
                "arguments", arguments != null ? arguments : ""
        ));
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.TOOL_CALL.name())
                .data(data)
                .done(false)
                .build();
    }

    /**
     * Tool result block: data is JSON {"toolName":"...", "result":"..."}.
     */
    public static ChatResponseBlock toolResult(String toolName, String result) {
        String data = JsonUtil.object2json(Map.of(
                "toolName", toolName != null ? toolName : "",
                "result", result != null ? result : ""
        ));
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.TOOL_RESULT.name())
                .data(data)
                .done(false)
                .build();
    }

}
