package edu.zsc.ai.config.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.FinishReason;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

@Slf4j
public class TerminalToolAwareStreamingChatModel implements StreamingChatModel {

    private final StreamingChatModel delegate;
    private final TerminalToolResultPolicy terminalToolResultPolicy;

    public TerminalToolAwareStreamingChatModel(
            StreamingChatModel delegate,
            TerminalToolResultPolicy terminalToolResultPolicy) {
        this.delegate = delegate;
        this.terminalToolResultPolicy = terminalToolResultPolicy;
    }

    @Override
    public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
        if (terminalToolResultPolicy.shouldShortCircuit(request)) {
            log.debug("Skip follow-up LLM request after successful terminal tool result.");
            handler.onCompleteResponse(ChatResponse.builder()
                    .aiMessage(AiMessage.from(""))
                    .finishReason(FinishReason.STOP)
                    .build());
            return;
        }

        delegate.chat(request, handler);
    }

    @Override
    public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
        chat(request, handler);
    }

    @Override
    public ChatRequestParameters defaultRequestParameters() {
        return delegate.defaultRequestParameters();
    }

    @Override
    public List<ChatModelListener> listeners() {
        return delegate.listeners();
    }

    @Override
    public ModelProvider provider() {
        return delegate.provider();
    }

    @Override
    public Set<Capability> supportedCapabilities() {
        return delegate.supportedCapabilities();
    }
}
