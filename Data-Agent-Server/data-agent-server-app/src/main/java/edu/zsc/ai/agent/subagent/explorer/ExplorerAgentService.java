package edu.zsc.ai.agent.subagent.explorer;

import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * LangChain4j AiService interface for Explorer SubAgent.
 * Discovers database schema using SearchObjects and GetObjectDetail tools.
 * Uses AiServices.builder() (not AgentBuilder) to avoid ThreadLocal NPE on streaming callbacks.
 */
public interface ExplorerAgentService {

    @UserMessage("{{instruction}}")
    TokenStream explore(@V("instruction") String instruction, InvocationParameters parameters);
}
