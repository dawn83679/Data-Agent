package edu.zsc.ai.domain.service.agent;

import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.TokenStream;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.context.AgentRequestContextInfo;
import edu.zsc.ai.context.RequestContextInfo;

/**
 * Immutable snapshot of all state prepared for a single chat request.
 */
public record ChatSession(
        String modelName,
        AgentModeEnum agentMode,
        ReActAgent agent,
        String memoryId,
        String enrichedMessage,
        InvocationParameters parameters,
        Long conversationId,
        RequestContextInfo requestContextSnapshot,
        AgentRequestContextInfo agentRequestContextSnapshot
) {
    /**
     * Start the agent chat, unpacking the session's own fields.
     */
    public TokenStream startChat() {
        return agent.chat(memoryId, enrichedMessage, parameters);
    }
}
