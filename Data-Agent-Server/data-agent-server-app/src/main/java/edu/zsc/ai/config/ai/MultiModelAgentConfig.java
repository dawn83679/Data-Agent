package edu.zsc.ai.config.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import edu.zsc.ai.agent.tool.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ask.AskUserConfirmTool;
import edu.zsc.ai.agent.tool.chart.ChartTool;
import edu.zsc.ai.agent.tool.plan.ExitPlanModeTool;
import edu.zsc.ai.agent.tool.sql.ExecuteSqlTool;

import dev.langchain4j.community.model.dashscope.QwenChatRequestParameters;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.agent.ReActAgentProvider;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import edu.zsc.ai.common.enums.ai.PromptLanguageEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configures multiple StreamingChatModel beans per supported model (ModelEnum),
 * and provides ReActAgentProvider for runtime selection by request model/language.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(QwenProperties.class)
public class MultiModelAgentConfig {

    /**
     * Static prompt cache: keyed by "language::mode" (e.g. "en::agent", "zh::plan").
     * Loaded once at class initialization, then reused by all agent beans.
     */
    private static final Map<String, String> SYSTEM_PROMPTS = buildPromptCache();

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

    /**
     * Tool classes completely disabled in Plan mode — not registered in the agent's tool list.
     */
    private static final Set<Class<?>> PLAN_MODE_DISABLED_TOOL_CLASSES = Set.of(
            ExecuteSqlTool.class,
            ChartTool.class,
            AskUserConfirmTool.class
    );

    /**
     * Tool classes disabled in Agent mode — Plan-only tools that should not appear in Agent mode.
     */
    private static final Set<Class<?>> AGENT_MODE_DISABLED_TOOL_CLASSES = Set.of(
            ExitPlanModeTool.class
    );

    private ReActAgent buildAgent(StreamingChatModel streamingChatModel,
                                  ChatMemoryProvider chatMemoryProvider,
                                  List<Object> agentTools,
                                  AgentModeEnum mode,
                                  String systemPrompt) {
        List<Object> tools;
        if (mode == AgentModeEnum.PLAN) {
            tools = agentTools.stream()
                    .filter(tool -> !PLAN_MODE_DISABLED_TOOL_CLASSES.contains(tool.getClass()))
                    .toList();
        } else {
            tools = agentTools.stream()
                    .filter(tool -> !AGENT_MODE_DISABLED_TOOL_CLASSES.contains(tool.getClass()))
                    .toList();
        }
        return AiServices.builder(ReActAgent.class)
                .streamingChatModel(streamingChatModel)
                .systemMessage(systemPrompt)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(tools)
                .build();
    }

    @Bean
    @Primary
    public ReActAgentProvider reActAgentProvider(
            @Qualifier("streamingChatModelQwen3Max") StreamingChatModel streamingChatModelQwen3Max,
            @Qualifier("streamingChatModelQwen3MaxThinking") StreamingChatModel streamingChatModelQwen3MaxThinking,
            @Qualifier("streamingChatModelQwenPlus") StreamingChatModel streamingChatModelQwenPlus,
            ChatMemoryProvider chatMemoryProvider,
            @Qualifier("agentTools") List<Object> agentTools) {
        Map<String, StreamingChatModel> modelsByName = Map.of(
                ModelEnum.QWEN3_MAX.getModelName(), streamingChatModelQwen3Max,
                ModelEnum.QWEN3_MAX_THINKING.getModelName(), streamingChatModelQwen3MaxThinking,
                ModelEnum.QWEN_PLUS.getModelName(), streamingChatModelQwenPlus
        );
        Map<String, ReActAgent> dynamicAgentCache = new ConcurrentHashMap<>();

        return (modelName, language, agentMode) -> {
            PromptLanguageEnum promptLanguage = PromptLanguageEnum.fromRequestLanguage(language);
            AgentModeEnum mode = AgentModeEnum.fromRequest(agentMode);
            StreamingChatModel model = modelsByName.get(modelName);
            if (model == null) {
                throw new IllegalArgumentException(
                        "No StreamingChatModel configured for model=" + modelName + ", language=" + promptLanguage.getCode());
            }
            String cacheKey = modelName + "::" + promptLanguage.getCode() + "::" + mode.getCode();
            return dynamicAgentCache.computeIfAbsent(cacheKey, key -> {
                log.info("Create ReActAgent dynamically: model={}, language={}, mode={}", modelName, promptLanguage.getCode(), mode.getCode());
                return buildAgent(model, chatMemoryProvider, agentTools, mode, systemPrompt(promptLanguage, mode));
            });
        };
    }

    private static String systemPrompt(PromptLanguageEnum language, AgentModeEnum mode) {
        return SYSTEM_PROMPTS.get(promptCacheKey(language, mode));
    }

    private static String promptCacheKey(PromptLanguageEnum language, AgentModeEnum mode) {
        return language.getCode() + "::" + mode.getCode();
    }

    private static Map<String, String> buildPromptCache() {
        Map<String, String> cache = new ConcurrentHashMap<>();
        for (PromptLanguageEnum lang : PromptLanguageEnum.values()) {
            for (AgentModeEnum mode : AgentModeEnum.values()) {
                String resource = lang.getSystemPromptResource(mode);
                cache.put(promptCacheKey(lang, mode), loadSystemPrompt(resource));
            }
        }
        return cache;
    }

    private static String loadSystemPrompt(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load system prompt resource: " + resourcePath, e);
        }
    }
}
