package edu.zsc.ai.domain.event;

import org.springframework.context.ApplicationEvent;

public class ConversationMemoryAutoWriteRequestedEvent extends ApplicationEvent {

    private final Long conversationId;

    public ConversationMemoryAutoWriteRequestedEvent(Object source, Long conversationId) {
        super(source);
        this.conversationId = conversationId;
    }

    public Long getConversationId() {
        return conversationId;
    }
}
