package edu.zsc.ai.agent.subagent;

import dev.langchain4j.memory.ChatMemory;

/**
 * Creates temporary, isolated chat memory for sub-agent invocations.
 * Each call produces an independent in-memory buffer with no message window limit.
 */
public final class SubAgentMemoryFactory {

    private SubAgentMemoryFactory() {
    }

    public static ChatMemory createTemporary() {
        return new InMemorySubAgentChatMemory();
    }
}
