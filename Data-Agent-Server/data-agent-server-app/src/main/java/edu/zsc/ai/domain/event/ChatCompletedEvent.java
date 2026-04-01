package edu.zsc.ai.domain.event;

import org.springframework.context.ApplicationEvent;

public class ChatCompletedEvent extends ApplicationEvent {

    private final Long conversationId;
    private final Integer outputTokens;
    private final Integer totalTokens;

    public ChatCompletedEvent(Object source,
                              Long conversationId,
                              Integer outputTokens,
                              Integer totalTokens) {
        super(source);
        this.conversationId = conversationId;
        this.outputTokens = outputTokens;
        this.totalTokens = totalTokens;
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
}
