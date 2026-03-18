package edu.zsc.ai.agent.subagent;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Unbounded in-memory chat memory for a single sub-agent invocation.
 * A sub-agent call is short-lived and isolated, so we keep the full message list
 * instead of applying a sliding window.
 */
public final class InMemorySubAgentChatMemory implements ChatMemory {

    private final Object id = UUID.randomUUID().toString();
    private final List<ChatMessage> messages = new ArrayList<>();

    @Override
    public Object id() {
        return id;
    }

    @Override
    public synchronized void add(ChatMessage chatMessage) {
        messages.add(chatMessage);
    }

    @Override
    public synchronized void set(Iterable<ChatMessage> chatMessages) {
        messages.clear();
        for (ChatMessage chatMessage : chatMessages) {
            messages.add(chatMessage);
        }
    }

    @Override
    public synchronized List<ChatMessage> messages() {
        return List.copyOf(messages);
    }

    @Override
    public synchronized void clear() {
        messages.clear();
    }
}
