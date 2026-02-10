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

    /** JSON keys in tool call/result block data (must match frontend ToolCallData / ToolResultData). */
    public static final String DATA_KEY_ID = "id";
    public static final String DATA_KEY_TOOL_NAME = "toolName";
    public static final String DATA_KEY_ARGUMENTS = "arguments";
    public static final String DATA_KEY_RESULT = "result";
    /** True when tool execution failed (ToolExecutionResult.isError). */
    public static final String DATA_KEY_ERROR = "error";

    private static final String EMPTY = "";

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
     * Tool call block: data is JSON {"id":"...", "toolName":"...", "arguments":"..."}.
     * id is optional (from LangChain4j ToolExecutionRequest / PartialToolCall); used to merge streaming chunks and pair with TOOL_RESULT.
     */
    public static ChatResponseBlock toolCall(String id, String toolName, String arguments) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        if (id != null && !id.isEmpty()) {
            map.put(DATA_KEY_ID, id);
        }
        map.put(DATA_KEY_TOOL_NAME, toolName != null ? toolName : EMPTY);
        map.put(DATA_KEY_ARGUMENTS, arguments != null ? arguments : EMPTY);
        String data = JsonUtil.object2json(map);
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.TOOL_CALL.name())
                .data(data)
                .done(false)
                .build();
    }

    /**
     * Tool result block: data is JSON {"id":"...", "toolName":"...", "result":"...", "error": true|false}.
     * id matches the tool call id for pairing. error is true when tool execution failed (ToolExecution.hasFailed()).
     */
    public static ChatResponseBlock toolResult(String id, String toolName, String result, boolean isError) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        if (id != null && !id.isEmpty()) {
            map.put(DATA_KEY_ID, id);
        }
        map.put(DATA_KEY_TOOL_NAME, toolName != null ? toolName : EMPTY);
        map.put(DATA_KEY_RESULT, result != null ? result : EMPTY);
        map.put(DATA_KEY_ERROR, isError);
        String data = JsonUtil.object2json(map);
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.TOOL_RESULT.name())
                .data(data)
                .done(false)
                .build();
    }

}
