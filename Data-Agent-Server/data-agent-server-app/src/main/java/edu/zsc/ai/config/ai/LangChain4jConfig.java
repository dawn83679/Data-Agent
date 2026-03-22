package edu.zsc.ai.config.ai;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shared LangChain4j config (memory, etc.). ReActAgent beans are defined in
 * {@link AgentManager} so that we can select by request model.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class LangChain4jConfig {

    /** Effectively disables count-based eviction; conversation compaction is handled separately. */
    public static final int MAX_MEMORY_MESSAGES = Integer.MAX_VALUE;

    private final ChatMemoryStore chatMemoryStore;

    @Bean
    @ConditionalOnMissingBean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(MAX_MEMORY_MESSAGES)
                .chatMemoryStore(chatMemoryStore)
                .build();
    }
}
