package edu.zsc.ai.domain.model.dto.response.agent;

import edu.zsc.ai.common.enums.ai.MessageBlockEnum;
import edu.zsc.ai.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
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
    /** True when tool arguments are still streaming (partial), false when complete */
    public static final String DATA_KEY_STREAMING = "streaming";
    public static final String DATA_KEY_RUN_ID = "runId";
    public static final String DATA_KEY_TASK_ID = "taskId";
    public static final String DATA_KEY_TASKS = "tasks";
    public static final String DATA_KEY_AGENT_ROLE = "agentRole";
    public static final String DATA_KEY_TITLE = "title";
    public static final String DATA_KEY_STATUS = "status";
    public static final String DATA_KEY_SUMMARY = "summary";
    public static final String DATA_KEY_DETAILS = "details";
    public static final String DATA_KEY_REQUIRES_APPROVAL = "requiresApproval";
    public static final String DATA_KEY_CONTENT = "content";

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
     * End-of-stream block: done=true. conversationId can be injected by stream mapping.
     */
    public static ChatResponseBlock doneBlock() {
        return ChatResponseBlock.builder()
                .done(true)
                .build();
    }

    /**
     * Tool call block: data is JSON {"id":"...", "toolName":"...", "arguments":"..."}.
     * id is optional (from LangChain4j ToolExecutionRequest / PartialToolCall); used to merge streaming chunks and pair with TOOL_RESULT.
     */
    public static ChatResponseBlock toolCall(String id, String toolName, String arguments) {
        return toolCall(id, toolName, arguments, null, null, null, null);
    }

    /**
     * Tool call block with optional streaming indicator:
     * data is JSON {"id":"...", "toolName":"...", "arguments":"...", "streaming": true|false}.
     */
    public static ChatResponseBlock toolCall(String id, String toolName, String arguments, Boolean streaming) {
        return toolCall(id, toolName, arguments, streaming, null, null, null);
    }

    public static ChatResponseBlock toolCall(
            String id,
            String toolName,
            String arguments,
            Boolean streaming,
            Long runId,
            Long taskId,
            String agentRole) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        if (id != null && !id.isEmpty()) {
            map.put(DATA_KEY_ID, id);
        }
        map.put(DATA_KEY_TOOL_NAME, toolName != null ? toolName : EMPTY);
        map.put(DATA_KEY_ARGUMENTS, arguments != null ? arguments : EMPTY);
        if (streaming != null) {
            map.put(DATA_KEY_STREAMING, streaming);
        }
        if (runId != null) {
            map.put(DATA_KEY_RUN_ID, runId);
        }
        if (taskId != null) {
            map.put(DATA_KEY_TASK_ID, taskId);
        }
        if (agentRole != null && !agentRole.isEmpty()) {
            map.put(DATA_KEY_AGENT_ROLE, agentRole);
        }
        String data = JsonUtil.object2json(map);
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.TOOL_CALL.name())
                .data(data)
                .done(false)
                .build();
    }

    /**
     * Status block: lightweight notification to the frontend (e.g. "compressing").
     * data is a status key that the frontend maps to a user-visible message.
     */
    public static ChatResponseBlock status(String statusKey) {
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.STATUS.name())
                .data(statusKey)
                .done(false)
                .build();
    }

    /**
     * Tool result block: data is JSON {"id":"...", "toolName":"...", "result":"...", "error": true|false}.
     * id matches the tool call id for pairing. error is true when tool execution failed (ToolExecution.hasFailed()).
     */
    public static ChatResponseBlock toolResult(String id, String toolName, String result, boolean isError) {
        return toolResult(id, toolName, result, isError, null, null, null);
    }

    public static ChatResponseBlock toolResult(
            String id,
            String toolName,
            String result,
            boolean isError,
            Long runId,
            Long taskId,
            String agentRole) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (id != null && !id.isEmpty()) {
            map.put(DATA_KEY_ID, id);
        }
        map.put(DATA_KEY_TOOL_NAME, toolName != null ? toolName : EMPTY);
        map.put(DATA_KEY_RESULT, result != null ? result : EMPTY);
        map.put(DATA_KEY_ERROR, isError);
        if (runId != null) {
            map.put(DATA_KEY_RUN_ID, runId);
        }
        if (taskId != null) {
            map.put(DATA_KEY_TASK_ID, taskId);
        }
        if (agentRole != null && !agentRole.isEmpty()) {
            map.put(DATA_KEY_AGENT_ROLE, agentRole);
        }
        String data = JsonUtil.object2json(map);
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.TOOL_RESULT.name())
                .data(data)
                .done(false)
                .build();
    }

    public static ChatResponseBlock taskPlan(Long runId, String title, java.util.List<Map<String, Object>> tasks) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(DATA_KEY_RUN_ID, runId);
        map.put(DATA_KEY_TITLE, title != null ? title : EMPTY);
        map.put(DATA_KEY_TASKS, tasks != null ? tasks : java.util.List.of());
        return block(MessageBlockEnum.TASK_PLAN.name(), JsonUtil.object2json(map));
    }

    public static ChatResponseBlock taskStart(
            Long runId,
            Long taskId,
            String agentRole,
            String title,
            String summary) {
        return taskBlock(MessageBlockEnum.TASK_START.name(), runId, taskId, agentRole, title, "running", summary, null, false);
    }

    public static ChatResponseBlock taskStatus(
            Long runId,
            Long taskId,
            String agentRole,
            String title,
            String status,
            String summary) {
        return taskBlock(MessageBlockEnum.TASK_STATUS.name(), runId, taskId, agentRole, title, status, summary, null, false);
    }

    public static ChatResponseBlock taskResult(
            Long runId,
            Long taskId,
            String agentRole,
            String title,
            String status,
            String summary,
            String details) {
        return taskBlock(MessageBlockEnum.TASK_RESULT.name(), runId, taskId, agentRole, title, status, summary, details, false);
    }

    public static ChatResponseBlock taskApproval(
            Long runId,
            Long taskId,
            String agentRole,
            String title,
            String summary,
            String details) {
        return taskBlock(MessageBlockEnum.TASK_APPROVAL.name(), runId, taskId, agentRole, title, "waiting_approval", summary, details, true);
    }

    public static ChatResponseBlock taskText(
            Long runId,
            Long taskId,
            String agentRole,
            String content,
            boolean streaming) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (runId != null) {
            map.put(DATA_KEY_RUN_ID, runId);
        }
        if (taskId != null) {
            map.put(DATA_KEY_TASK_ID, taskId);
        }
        map.put(DATA_KEY_AGENT_ROLE, agentRole != null ? agentRole : EMPTY);
        map.put(DATA_KEY_CONTENT, content != null ? content : EMPTY);
        map.put(DATA_KEY_STREAMING, streaming);
        return block(MessageBlockEnum.TASK_TEXT.name(), JsonUtil.object2json(map));
    }

    private static ChatResponseBlock taskBlock(
            String type,
            Long runId,
            Long taskId,
            String agentRole,
            String title,
            String status,
            String summary,
            String details,
            boolean requiresApproval) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (runId != null) {
            map.put(DATA_KEY_RUN_ID, runId);
        }
        if (taskId != null) {
            map.put(DATA_KEY_TASK_ID, taskId);
        }
        map.put(DATA_KEY_AGENT_ROLE, agentRole != null ? agentRole : EMPTY);
        map.put(DATA_KEY_TITLE, title != null ? title : EMPTY);
        map.put(DATA_KEY_STATUS, status != null ? status : EMPTY);
        map.put(DATA_KEY_SUMMARY, summary != null ? summary : EMPTY);
        if (details != null && !details.isEmpty()) {
            map.put(DATA_KEY_DETAILS, details);
        }
        map.put(DATA_KEY_REQUIRES_APPROVAL, requiresApproval);
        return block(type, JsonUtil.object2json(map));
    }

    private static ChatResponseBlock block(String type, String data) {
        return ChatResponseBlock.builder()
                .type(type)
                .data(data)
                .done(false)
                .build();
    }

}
