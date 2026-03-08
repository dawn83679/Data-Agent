package edu.zsc.ai.config.ai;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.agent.ReActAgentProvider;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.PromptLanguageEnum;
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
            PromptLanguageEnum promptLanguage = PromptLanguageEnum.fromRequestLanguage(language);
            AgentModeEnum mode = AgentModeEnum.fromRequest(agentMode);
            StreamingChatModel model = modelsByName.get(modelName);
            if (model == null) {
                throw new IllegalArgumentException(
                        "No StreamingChatModel configured for model=" + modelName);
            }
            String cacheKey = modelName + "::" + promptLanguage.getCode() + "::" + mode.getCode();
            return dynamicAgentCache.computeIfAbsent(cacheKey, key -> {
                log.info("Create ReActAgent dynamically: model={}, language={}, mode={}",
                        modelName, promptLanguage.getCode(), mode.getCode());
                String systemPrompt = PromptConfig.getSystemPrompt(promptLanguage);
                return buildAgent(model, mode, systemPrompt);
            });
        };
    }

    private ReActAgent buildAgent(StreamingChatModel model,
                                   AgentModeEnum mode,
                                   String systemPrompt) {
        List<Object> tools = agentToolConfig.filterTools(agentTools, mode);

        return AiServices.builder(ReActAgent.class)
                .streamingChatModel(model)
                .systemMessage(systemPrompt)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(tools)
                .build();
    }
}
