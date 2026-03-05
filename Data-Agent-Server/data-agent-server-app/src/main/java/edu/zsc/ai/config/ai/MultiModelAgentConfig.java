package edu.zsc.ai.config.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import edu.zsc.ai.agent.tool.AgentTool;

import dev.langchain4j.community.model.dashscope.QwenChatRequestParameters;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.agent.ReActAgentProvider;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configures multiple StreamingChatModel and ReActAgent beans per supported model (ModelEnum),
 * and provides ReActAgentProvider for runtime selection by request model name.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(QwenProperties.class)
public class MultiModelAgentConfig {

    private final QwenProperties qwenProperties;

    @Bean("agentTools")
    public List<Object> agentTools(ApplicationContext context) {
        return new ArrayList<>(context.getBeansWithAnnotation(AgentTool.class).values());
    }

    @Bean("streamingChatModelQwen3Max")
    public StreamingChatModel streamingChatModelQwen3Max() {
        return QwenStreamingChatModel.builder()
                .apiKey(qwenProperties.getApiKey())
                .modelName(ModelEnum.QWEN3_MAX.getModelName())
                .defaultRequestParameters(
                        QwenChatRequestParameters.builder()
                                .enableThinking(false)
                                .enableSanitizeMessages(false)
                                .build())
                .build();
    }

    @Bean("streamingChatModelQwen3MaxThinking")
    public StreamingChatModel streamingChatModelQwen3MaxThinking() {
        return QwenStreamingChatModel.builder()
                .apiKey(qwenProperties.getApiKey())
                .modelName(ModelEnum.QWEN3_MAX.getModelName())
                .defaultRequestParameters(
                        QwenChatRequestParameters.builder()
                                .enableThinking(true)
                                .thinkingBudget(qwenProperties.getParameters().getThinkingBudget())
                                .enableSanitizeMessages(false)
                                .build())
                .build();
    }

    @Bean("streamingChatModelQwenPlus")
    public StreamingChatModel streamingChatModelQwenPlus() {
        return QwenStreamingChatModel.builder()
                .apiKey(qwenProperties.getApiKey())
                .modelName(ModelEnum.QWEN_PLUS.getModelName())
                .defaultRequestParameters(
                        QwenChatRequestParameters.builder()
                                .enableThinking(false)
                                .enableSanitizeMessages(false)
                                .build())
                .build();
    }

    @Bean("reActAgentQwen3Max")
    public ReActAgent reActAgentQwen3Max(
            @Qualifier("streamingChatModelQwen3Max") StreamingChatModel streamingChatModel,
            ChatMemoryProvider chatMemoryProvider,
            @Qualifier("agentTools") List<Object> agentTools) {
        return buildAgent(streamingChatModel, chatMemoryProvider, agentTools);
    }

    @Bean("reActAgentQwen3MaxThinking")
    public ReActAgent reActAgentQwen3MaxThinking(
            @Qualifier("streamingChatModelQwen3MaxThinking") StreamingChatModel streamingChatModel,
            ChatMemoryProvider chatMemoryProvider,
            @Qualifier("agentTools") List<Object> agentTools) {
        return buildAgent(streamingChatModel, chatMemoryProvider, agentTools);
    }

    @Bean("reActAgentQwenPlus")
    public ReActAgent reActAgentQwenPlus(
            @Qualifier("streamingChatModelQwenPlus") StreamingChatModel streamingChatModel,
            ChatMemoryProvider chatMemoryProvider,
            @Qualifier("agentTools") List<Object> agentTools) {
        return buildAgent(streamingChatModel, chatMemoryProvider, agentTools);
    }

    private ReActAgent buildAgent(StreamingChatModel streamingChatModel,
                                  ChatMemoryProvider chatMemoryProvider,
                                  @Qualifier("agentTools") List<Object> agentTools) {
        return AiServices.builder(ReActAgent.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(agentTools)
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
