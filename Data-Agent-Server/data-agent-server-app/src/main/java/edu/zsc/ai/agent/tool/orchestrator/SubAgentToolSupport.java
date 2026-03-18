package edu.zsc.ai.agent.tool.orchestrator;

import edu.zsc.ai.agent.subagent.SubAgentTextSupport;
import edu.zsc.ai.agent.subagent.contract.ExploreObject;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;

import java.util.List;
import java.util.UUID;

/**
 * Shared utilities for SubAgent orchestrator tools.
 * Trace/span management has been moved to SubAgentObservabilityListener (AgentListener).
 */
abstract class SubAgentToolSupport {

    protected Long parseConversationId() {
        try {
            Long id = RequestContext.getConversationId();
            return id != null ? id : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    protected String preview(String value) {
        return SubAgentTextSupport.preview(value);
    }

    protected String summarizeObjects(List<ExploreObject> objects) {
        return SubAgentTextSupport.summarizeObjects(objects);
    }

    protected Throwable rootCause(Throwable throwable) {
        return SubAgentTextSupport.rootCause(throwable);
    }

    protected String rootCauseMessage(Throwable throwable) {
        return SubAgentTextSupport.rootCauseMessage(throwable);
    }

    protected long resolveTimeoutSeconds(Long requestedTimeoutSeconds, long defaultTimeoutSeconds) {
        return requestedTimeoutSeconds != null && requestedTimeoutSeconds > 0
                ? requestedTimeoutSeconds
                : defaultTimeoutSeconds;
    }

    protected String buildTaskId(String prefix, RequestContextInfo requestContextSnapshot) {
        return prefix + "-" + (requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : "0")
                + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
