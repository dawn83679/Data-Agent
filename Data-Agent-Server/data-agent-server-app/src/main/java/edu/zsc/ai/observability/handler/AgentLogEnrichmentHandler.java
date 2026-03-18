package edu.zsc.ai.observability.handler;

import edu.zsc.ai.context.AgentExecutionContext;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.AgentRequestContextInfo;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.observability.AgentLogEvent;
import edu.zsc.ai.observability.AgentLogHandler;
import org.apache.commons.collections4.MapUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AgentLogEnrichmentHandler implements AgentLogHandler {

    @Override
    public void handle(AgentLogEvent event) {
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }
        RequestContextInfo requestContext = RequestContext.snapshot();
        AgentRequestContextInfo agentRequestContext = AgentRequestContext.snapshot();
        if (event.getConversationId() == null && requestContext != null) {
            event.setConversationId(requestContext.getConversationId());
        }
        if (event.getAgentType() == null && agentRequestContext != null) {
            event.setAgentType(agentRequestContext.getAgentType());
        }
        if (event.getParentToolCallId() == null) {
            event.setParentToolCallId(AgentExecutionContext.getParentToolCallId());
        }
        if (event.getTaskId() == null) {
            event.setTaskId(AgentExecutionContext.getTaskId());
        }
        if (MapUtils.isEmpty(event.getPayload())) {
            event.setPayload(new LinkedHashMap<>());
        }
    }
}
