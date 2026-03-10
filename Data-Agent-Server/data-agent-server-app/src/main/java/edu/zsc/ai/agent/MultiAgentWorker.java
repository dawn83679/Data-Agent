package edu.zsc.ai.agent;

import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface MultiAgentWorker {

    TokenStream run(@UserMessage String message, InvocationParameters parameters);
}
