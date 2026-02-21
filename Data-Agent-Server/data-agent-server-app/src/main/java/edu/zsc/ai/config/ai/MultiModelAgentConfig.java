package edu.zsc.ai.config.ai;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.community.model.dashscope.QwenChatRequestParameters;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolExecutor;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.agent.ReActAgentProvider;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import edu.zsc.ai.tool.AskUserQuestionTool;
import edu.zsc.ai.tool.TableTool;
import edu.zsc.ai.tool.TodoTool;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configures multiple StreamingChatModel and ReActAgent beans per supported model (ModelEnum),
 * and provides ReActAgentProvider for runtime selection by request model name.
 */
@Configuration
@EnableConfigurationProperties(MultiModelAgentConfig.DashScopeProperties.class)
@Slf4j
@RequiredArgsConstructor
public class MultiModelAgentConfig {

    private static final int THINKING_BUDGET = 1000;

    private final DashScopeProperties dashScopeProperties;

    @ConfigurationProperties(prefix = "langchain4j.community.dashscope")
    @Data
    public static class DashScopeProperties {
        private ChatModel chatModel = new ChatModel();
        private StreamingChatModel streamingChatModel = new StreamingChatModel();
        
        @Data
        public static class ChatModel {
            private String apiKey;
            private String modelName;
        }
        
        @Data
        public static class StreamingChatModel {
            private String apiKey;
            private String modelName;
        }
    }

    @Bean("streamingChatModelQwen3Max")
    public StreamingChatModel streamingChatModelQwen3Max() {
        return QwenStreamingChatModel.builder()
                .apiKey(dashScopeProperties.getStreamingChatModel().getApiKey())
                .modelName(ModelEnum.QWEN3_MAX.getModelName())
                .defaultRequestParameters(
                        QwenChatRequestParameters.builder()
                                .enableThinking(false)
                                .build())
                .build();
    }

    /** Same API model as qwen3-max, but with thinking enabled (for frontend option qwen3-max-thinking). */
    @Bean("streamingChatModelQwen3MaxThinking")
    public StreamingChatModel streamingChatModelQwen3MaxThinking() {
        return QwenStreamingChatModel.builder()
                .apiKey(dashScopeProperties.getStreamingChatModel().getApiKey())
                .modelName(ModelEnum.QWEN3_MAX.getModelName())
                .defaultRequestParameters(
                        QwenChatRequestParameters.builder()
                                .enableThinking(true)
                                .thinkingBudget(THINKING_BUDGET)
                                .build())
                .build();
    }

    @Bean("streamingChatModelQwenPlus")
    public StreamingChatModel streamingChatModelQwenPlus() {
        return QwenStreamingChatModel.builder()
                .apiKey(dashScopeProperties.getStreamingChatModel().getApiKey())
                .modelName(ModelEnum.QWEN_PLUS.getModelName())
                .defaultRequestParameters(
                        QwenChatRequestParameters.builder()
                                .enableThinking(false)
                                .build())
                .build();
    }

    @Bean("reActAgentQwen3Max")
    public ReActAgent reActAgentQwen3Max(
            @Qualifier("streamingChatModelQwen3Max") StreamingChatModel streamingChatModel,
            ChatMemoryProvider chatMemoryProvider,
            TodoTool todoTool,
            TableTool tableTool,
            AskUserQuestionTool askUserQuestionTool,
            @Qualifier("mcpToolProvider") Map<ToolSpecification, ToolExecutor> mcpToolProvider) {
        return AiServices.builder(ReActAgent.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(todoTool, tableTool, askUserQuestionTool)
                .tools(mcpToolProvider)  // Add MCP tools via map
                .build();
    }

    @Bean("reActAgentQwen3MaxThinking")
    public ReActAgent reActAgentQwen3MaxThinking(
            @Qualifier("streamingChatModelQwen3MaxThinking") StreamingChatModel streamingChatModel,
            ChatMemoryProvider chatMemoryProvider,
            TodoTool todoTool,
            TableTool tableTool,
            AskUserQuestionTool askUserQuestionTool,
            @Qualifier("mcpToolProvider") Map<ToolSpecification, ToolExecutor> mcpToolProvider) {
        return AiServices.builder(ReActAgent.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(todoTool, tableTool, askUserQuestionTool)
                .tools(mcpToolProvider)  // Add MCP tools via map
                .build();
    }

    @Bean("reActAgentQwenPlus")
    public ReActAgent reActAgentQwenPlus(
            @Qualifier("streamingChatModelQwenPlus") StreamingChatModel streamingChatModel,
            ChatMemoryProvider chatMemoryProvider,
            TodoTool todoTool,
            TableTool tableTool,
            AskUserQuestionTool askUserQuestionTool,
            @Qualifier("mcpToolProvider") Map<ToolSpecification, ToolExecutor> mcpToolProvider) {
        return AiServices.builder(ReActAgent.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(todoTool, tableTool, askUserQuestionTool)
                .tools(mcpToolProvider)  // Add MCP tools via map
                .build();
    }

    @Bean
    @Primary
    public ReActAgentProvider reActAgentProvider(
            @Qualifier("reActAgentQwen3Max") ReActAgent reActAgentQwen3Max,
            @Qualifier("reActAgentQwen3MaxThinking") ReActAgent reActAgentQwen3MaxThinking,
            @Qualifier("reActAgentQwenPlus") ReActAgent reActAgentQwenPlus) {
        Map<String, ReActAgent> byModel = Stream.of(
                Map.entry(ModelEnum.QWEN3_MAX.getModelName(), reActAgentQwen3Max),
                Map.entry(ModelEnum.QWEN3_MAX_THINKING.getModelName(), reActAgentQwen3MaxThinking),
                Map.entry(ModelEnum.QWEN_PLUS.getModelName(), reActAgentQwenPlus)
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return modelName -> {
            ReActAgent agent = byModel.get(modelName);
            if (agent == null) {
                throw new IllegalArgumentException("No ReActAgent configured for model: " + modelName);
            }
            return agent;
        };
    }
}