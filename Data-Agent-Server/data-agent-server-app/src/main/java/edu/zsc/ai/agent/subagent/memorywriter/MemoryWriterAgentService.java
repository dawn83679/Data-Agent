package edu.zsc.ai.agent.subagent.memorywriter;

import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Internal background memory writer agent.
 * Runs tool-assisted memory reads/writes without exposing those tool calls to the user chat stream.
 */
public interface MemoryWriterAgentService {

    @UserMessage("{{instruction}}")
    String write(@V("instruction") String instruction, InvocationParameters parameters);
}
