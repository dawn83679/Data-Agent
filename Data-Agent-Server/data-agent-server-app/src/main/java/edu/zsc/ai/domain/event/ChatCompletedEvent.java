package edu.zsc.ai.domain.event;

import org.springframework.context.ApplicationEvent;

public class ChatCompletedEvent extends ApplicationEvent {

    private final Long conversationId;
    private final Integer outputTokens;
    private final Integer totalTokens;
    private final boolean finalTurn;

    public ChatCompletedEvent(Object source,
                              Long conversationId,
                              Integer outputTokens,
                              Integer totalTokens,
                              boolean finalTurn) {
        super(source);
        this.conversationId = conversationId;
        this.outputTokens = outputTokens;
        this.totalTokens = totalTokens;
        this.finalTurn = finalTurn;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public boolean isFinalTurn() {
        return finalTurn;
    }
}
