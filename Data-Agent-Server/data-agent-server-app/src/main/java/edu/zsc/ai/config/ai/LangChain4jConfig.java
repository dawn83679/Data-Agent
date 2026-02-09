package edu.zsc.ai.config.ai;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.tool.TableTool;
import edu.zsc.ai.tool.TodoTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class LangChain4jConfig {

    /** Max number of tool invocations running concurrently. */
    public static final int TOOL_CONCURRENCY = 5;

    /** Max number of messages to retain in chat memory (no token counting, avoids DashScope Tokenization API). */
    public static final int MAX_MEMORY_MESSAGES = 50;

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

    /**
     * Executor for concurrent tool execution. At most {@value TOOL_CONCURRENCY} tools run at once.
     */
    @Bean(name = "toolExecutionExecutor")
    public Executor toolExecutionExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                TOOL_CONCURRENCY,
                TOOL_CONCURRENCY,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                r -> {
                    Thread t = new Thread(r, "agent-tool-exec-" + System.identityHashCode(r));
                    t.setDaemon(false);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        log.info("Tool execution executor created: max {} concurrent tools", TOOL_CONCURRENCY);
        return executor;
    }

    /**
     * ReActAgent built with AiServices.builder() and concurrent tool execution (max {@value TOOL_CONCURRENCY}).
     */
    @Bean
    @ConditionalOnMissingBean(ReActAgent.class)
    public ReActAgent reActAgent(
            StreamingChatModel streamingChatModel,
            ChatMemoryProvider chatMemoryProvider,
            TodoTool todoTool,
            TableTool tableTool,
            @Qualifier("toolExecutionExecutor") Executor toolExecutionExecutor) {
        return AiServices.builder(ReActAgent.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(todoTool, tableTool)
                .executeToolsConcurrently(toolExecutionExecutor)
                .build();
    }
}
