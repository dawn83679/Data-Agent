package edu.zsc.ai.agent.subagent;

import edu.zsc.ai.common.constant.InvocationContextConstant;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.config.ai.AiModelCatalog;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.RequestContext;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared execution helpers for concrete SubAgent implementations.
 */
public abstract class AbstractSubAgent<I, O> implements SubAgent<I, O> {

    protected Long resolveConversationId() {
        try {
            Long id = RequestContext.getConversationId();
            return id != null ? id : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    protected String resolveModelName(Logger logger, AiModelCatalog aiModelCatalog) {
        String modelName = AgentRequestContext.getModelName();
        if (modelName != null && !modelName.isBlank()) {
            return modelName;
        }
        logger.warn("No modelName in AgentRequestContext, falling back to default");
        return aiModelCatalog.defaultModelName();
    }

    protected Map<String, Object> createInvocationContext(AgentTypeEnum agentType) {
        Map<String, Object> invocationContext = new HashMap<>(RequestContext.toMap());
        invocationContext.putAll(AgentRequestContext.toMap());
        invocationContext.put(InvocationContextConstant.AGENT_TYPE, agentType.getCode());
        return invocationContext;
    }

    protected String preview(String value) {
        return SubAgentTextSupport.preview(value);
    }

    protected String summarizeObjects(java.util.List<edu.zsc.ai.agent.subagent.contract.ExploreObject> objects) {
        return SubAgentTextSupport.summarizeObjects(objects);
    }

    protected Throwable rootCause(Throwable throwable) {
        return SubAgentTextSupport.rootCause(throwable);
    }

    protected String rootCauseMessage(Throwable throwable) {
        return SubAgentTextSupport.rootCauseMessage(throwable);
    }

    protected String errorSummary(Throwable throwable, String fallbackMessage) {
        return SubAgentTextSupport.errorSummary(throwable, fallbackMessage);
    }

    protected String errorSummary(Throwable throwable, String fallbackMessage, Long timeoutSeconds) {
        return SubAgentTextSupport.errorSummary(throwable, fallbackMessage, timeoutSeconds);
    }

    protected long resolveTimeoutSeconds(Long requestedTimeoutSeconds, long defaultTimeoutSeconds) {
        return SubAgentTimeoutPolicy.normalizeTimeoutSeconds(requestedTimeoutSeconds, defaultTimeoutSeconds);
    }
}
