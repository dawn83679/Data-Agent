package edu.zsc.ai.observability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentLogEvent {

    private Instant timestamp;
    private AgentLogType type;
    private String loggerName;
    private Long conversationId;
    private String traceId;
    private String agentType;
    private String taskId;
    private String toolCallId;
    private String parentToolCallId;
    private Long elapsedMs;
    private String status;
    private String message;
    @Builder.Default
    private Map<String, Object> payload = new LinkedHashMap<>();
    private String errorClass;
    private String errorMessage;
    private String stackTrace;
}
