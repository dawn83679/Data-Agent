package edu.zsc.ai.agent;

import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;


public interface ReActAgent {

    @SystemMessage(fromResource = "prompt/system.md")
    TokenStream chat(@MemoryId String memoryId, @UserMessage String message, InvocationParameters parameters);
}
