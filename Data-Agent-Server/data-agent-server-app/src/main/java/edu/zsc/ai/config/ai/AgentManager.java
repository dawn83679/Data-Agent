package edu.zsc.ai.config.ai;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.agent.ReActAgentProvider;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.common.enums.ai.PromptEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class AgentManager {

    private final ChatMemoryProvider chatMemoryProvider;
    private final AgentToolConfig agentToolConfig;
    private final Map<String, StreamingChatModel> modelsByName;
    private final List<Object> agentTools;

    private final Map<String, ReActAgent> dynamicAgentCache = new ConcurrentHashMap<>();

    @Bean
    @Primary
    public ReActAgentProvider reActAgentProvider() {
        return (modelName, language, agentMode) -> {
            PromptEnum promptLanguage = PromptEnum.fromRequestLanguage(language);
            AgentModeEnum mode = AgentModeEnum.fromRequest(agentMode);
            StreamingChatModel model = modelsByName.get(modelName);
            if (model == null) {
                throw new IllegalArgumentException(
                        "No StreamingChatModel configured for model=" + modelName);
            }
            String cacheKey = buildCacheKey(modelName, promptLanguage.getCode(), mode.getCode(), AgentTypeEnum.MAIN);
            return dynamicAgentCache.computeIfAbsent(cacheKey, key -> {
                log.info("Create MainAgent dynamically: model={}, language={}, mode={}",
                        modelName, promptLanguage.getCode(), mode.getCode());
                String systemPrompt = PromptConfig.getPrompt(promptLanguage);
                return buildMainAgent(model, mode, systemPrompt);
            });
        };
    }

    private ReActAgent buildMainAgent(StreamingChatModel model,
                                       AgentModeEnum mode,
                                       String systemPrompt) {
        List<Object> tools = agentToolConfig.resolveMainTools(agentTools, mode);

        return AiServices.builder(ReActAgent.class)
                .streamingChatModel(model)
                .systemMessage(systemPrompt)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(agentToolConfig.buildToolExecutors(tools))
                .build();
    }

    /**
     * Cache key format: modelName::language::mode::agentType
     */
    static String buildCacheKey(String modelName, String language, String mode, AgentTypeEnum agentType) {
        return modelName + "::" + language + "::" + mode + "::" + agentType.getCode();
    }
}
