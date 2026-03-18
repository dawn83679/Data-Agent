package edu.zsc.ai.observability.handler;

import edu.zsc.ai.config.ai.AgentObservabilityProperties;
import edu.zsc.ai.observability.AgentLogEvent;
import edu.zsc.ai.observability.AgentLogHandler;
import edu.zsc.ai.observability.config.AgentObservabilityConfigProvider;
import edu.zsc.ai.observability.config.AgentObservabilitySettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 200)
@RequiredArgsConstructor
public class AgentConsoleHandler implements AgentLogHandler {

    private final AgentObservabilityConfigProvider configProvider;

    @Override
    public void handle(AgentLogEvent event) {
        AgentObservabilitySettings settings = configProvider.current();
        if (!settings.isEnabled() || !settings.isConsoleLogEnabled() || event == null) {
            return;
        }
        String summary = String.format(
                "[AgentLog] type=%s conv=%s agent=%s task=%s parentToolCallId=%s message=%s error=%s",
                event.getType(),
                event.getConversationId(),
                event.getAgentType(),
                event.getTaskId(),
                event.getParentToolCallId(),
                event.getMessage(),
                event.getErrorMessage());
        if (event.getErrorMessage() != null) {
            log.warn(summary);
            return;
        }
        log.info(summary);
    }
}
