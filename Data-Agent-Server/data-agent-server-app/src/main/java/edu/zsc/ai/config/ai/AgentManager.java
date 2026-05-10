package edu.zsc.ai.config.ai;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import edu.zsc.ai.agent.PreparedReActAgent;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.agent.ReActAgentProvider;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.common.enums.ai.PromptEnum;
import edu.zsc.ai.domain.service.agent.systemprompt.SystemPromptAssemblyContext;
import edu.zsc.ai.domain.service.agent.systemprompt.SystemPromptManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class AgentManager {

    private final ChatMemoryProvider chatMemoryProvider;
    private final AgentToolConfig agentToolConfig;
    private final Map<String, StreamingChatModel> modelsByName;
    private final List<Object> agentTools;
    private final AgentSkillConfig agentSkillConfig;
    private final SystemPromptManager systemPromptManager;
    private final TerminalToolResultPolicy terminalToolResultPolicy;

    @Bean
    @Primary
    public ReActAgentProvider reActAgentProvider() {
        return (modelName, language, agentMode) -> {
            AgentModeEnum mode = AgentModeEnum.fromRequest(agentMode);
            PromptEnum promptLanguage = resolveMainPrompt(language, mode);
            StreamingChatModel model = modelsByName.get(modelName);
            if (model == null) {
                throw new IllegalArgumentException(
                        "No StreamingChatModel configured for model=" + modelName);
            }
            log.info("Create MainAgent dynamically: model={}, language={}, mode={}",
                    modelName, promptLanguage.getCode(), mode.getCode());
            String systemPrompt = systemPromptManager.render(SystemPromptAssemblyContext.builder()
                            .promptEnum(promptLanguage)
                            .agentType(AgentTypeEnum.MAIN)
                            .agentMode(mode)
                            .modelName(modelName)
                            .language(promptLanguage.getCode())
                            .availableSkills(agentSkillConfig.resolveAvailableSkills(AgentTypeEnum.MAIN, mode))
                            .build())
                    .renderedPrompt();
            return new PreparedReActAgent(buildMainAgent(model, mode, systemPrompt), systemPrompt, promptLanguage);
        };
    }

    private ReActAgent buildMainAgent(StreamingChatModel model,
                                      AgentModeEnum mode,
                                      String systemPrompt) {
        List<Object> tools = agentToolConfig.resolveMainTools(agentTools, mode);
        AgentToolConfig.ToolBundle toolBundle = agentToolConfig.buildToolBundle(tools);
        StreamingChatModel effectiveModel = new TerminalToolAwareStreamingChatModel(model, terminalToolResultPolicy);

        return AiServices.builder(ReActAgent.class)
                .streamingChatModel(effectiveModel)
                .systemMessage(systemPrompt)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(toolBundle.executors(), toolBundle.immediateReturnToolNames())
                .build();
    }

    /**
     * Cache key format: modelName::language::mode::agentType
     */
    static String buildCacheKey(String modelName, String language, String mode, AgentTypeEnum agentType) {
        return modelName + "::" + language + "::" + mode + "::" + agentType.getCode();
    }

    private PromptEnum resolveMainPrompt(String language, AgentModeEnum mode) {
        if (mode != AgentModeEnum.PLAN) {
            return PromptEnum.ZH;
        }
        return PromptEnum.ZH_PLAN;
    }
}
