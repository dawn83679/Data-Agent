package edu.zsc.ai.observability;

import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.util.JsonUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public interface AgentLogService {

    void record(AgentLogEvent event);

    default void record(AgentLogType type, String loggerName, String message, Map<String, Object> payload) {
        record(AgentLogEvent.builder()
                .timestamp(Instant.now())
                .type(type)
                .loggerName(loggerName)
                .message(message)
                .payload(payload != null ? new LinkedHashMap<>(payload) : new LinkedHashMap<>())
                .build());
    }

    default void recordError(AgentLogType type, String loggerName, String message, Throwable throwable, Map<String, Object> payload) {
        record(AgentLogEvent.builder()
                .timestamp(Instant.now())
                .type(type)
                .loggerName(loggerName)
                .message(message)
                .payload(payload != null ? new LinkedHashMap<>(payload) : new LinkedHashMap<>())
                .errorClass(throwable != null ? throwable.getClass().getName() : null)
                .errorMessage(throwable != null ? throwable.getMessage() : null)
                .stackTrace(throwable != null ? ExceptionUtils.getStackTrace(throwable) : null)
                .build());
    }

    default void recordDebug(String loggerName, String eventName, Map<String, Object> payload) {
        Map<String, Object> data = payload != null ? new LinkedHashMap<>(payload) : new LinkedHashMap<>();
        data.putIfAbsent("eventName", eventName);
        record(AgentLogType.DEBUG_EVENT, loggerName, eventName, data);
    }

    default void recordChatBlock(Long conversationId, ChatResponseBlock block) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("blockType", block != null ? block.getType() : null);
        payload.put("done", block != null && block.isDone());
        payload.put("parentToolCallId", block != null ? block.getParentToolCallId() : null);
        payload.put("subAgentTaskId", block != null ? block.getSubAgentTaskId() : null);
        payload.put("blockData", block != null ? JsonUtil.object2json(block) : null);
        record(AgentLogEvent.builder()
                .timestamp(Instant.now())
                .type(AgentLogType.CHAT_BLOCK)
                .loggerName("ChatService")
                .conversationId(conversationId)
                .message(block != null ? block.getType() : "UNKNOWN")
                .payload(payload)
                .status(block != null && block.isDone() ? "done" : null)
                .build());
    }
}
