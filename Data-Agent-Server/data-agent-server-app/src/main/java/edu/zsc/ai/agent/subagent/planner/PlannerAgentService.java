package edu.zsc.ai.agent.subagent.planner;

import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * LangChain4j AiService interface for Planner SubAgent.
 * Generates SQL execution plans from natural language instructions and schema information.
 * Uses AiServices.builder() (not AgentBuilder) to avoid ThreadLocal NPE on streaming callbacks.
 */
public interface PlannerAgentService {

    @UserMessage("{{instruction}}")
    TokenStream plan(@V("instruction") String instruction, InvocationParameters parameters);
}
