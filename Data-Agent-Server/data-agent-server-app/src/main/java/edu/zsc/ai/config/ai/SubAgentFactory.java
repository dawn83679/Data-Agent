package edu.zsc.ai.config.ai;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.subagent.SubAgentMemoryFactory;
import edu.zsc.ai.agent.subagent.explorer.ExplorerAgentService;
import edu.zsc.ai.agent.subagent.memorywriter.MemoryWriterAgentService;
import edu.zsc.ai.agent.subagent.planner.PlannerAgentService;
import edu.zsc.ai.common.constant.AgentRuntimeLoggerNames;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.common.enums.ai.PromptEnum;
import edu.zsc.ai.domain.service.agent.systemprompt.SystemPromptAssemblyContext;
import edu.zsc.ai.domain.service.agent.systemprompt.SystemPromptManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Factory for building SubAgent instances (Explorer, Planner).
 * Uses AiServices.builder() (same as main agent) instead of AgentBuilder
 * to avoid LangChain4jManaged ThreadLocal NPE on OkHttp callback threads.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubAgentFactory {

    private static final Logger runtimeLog = LoggerFactory.getLogger(AgentRuntimeLoggerNames.PROMPT);

    private final Map<String, StreamingChatModel> modelsByName;
    private final Map<String, ChatModel> chatModelsByName;
    private final AgentToolConfig agentToolConfig;
    private final ApplicationContext applicationContext;
    private final AgentSkillConfig agentSkillConfig;
    private final SystemPromptManager systemPromptManager;

    /**
     * Build an Explorer SubAgent using AiServices.builder().
     * Each invocation creates a fresh agent with isolated temporary memory.
     */
    public ExplorerAgentService buildExplorerAgent(String modelName, Long conversationId) {
        StreamingChatModel model = resolveModel(modelName);
        List<Object> tools = agentToolConfig.resolveSubAgentTools(getAgentTools(), AgentTypeEnum.EXPLORER);
        AgentToolConfig.ToolBundle toolBundle = agentToolConfig.buildToolBundle(tools);
        ChatMemory memory = SubAgentMemoryFactory.createTemporary();
        String systemPrompt = renderSystemPrompt(PromptEnum.EXPLORER, AgentTypeEnum.EXPLORER, AgentModeEnum.AGENT, modelName);
        runtimeLog.info("subagent_prompt conversationId={} agentType={} modelName={} promptCode={} contentLength={} content=\n{}",
                conversationId,
                AgentTypeEnum.EXPLORER.getCode(),
                modelName,
                PromptEnum.EXPLORER.getCode(),
                systemPrompt.length(),
                systemPrompt);

        log.info("[SubAgentFactory] Building {} SubAgent, model={}, toolCount={}", AgentTypeEnum.EXPLORER, modelName, tools.size());
        return AiServices.builder(ExplorerAgentService.class)
                .streamingChatModel(model)
                .systemMessage(systemPrompt)
                .chatMemory(memory)
                .tools(toolBundle.executors(), toolBundle.immediateReturnToolNames())
                .build();
    }

    /**
     * Build a Planner SubAgent using AiServices.builder().
     * Each invocation creates a fresh agent with isolated temporary memory.
     */
    public PlannerAgentService buildPlannerAgent(String modelName, Long conversationId) {
        StreamingChatModel model = resolveModel(modelName);
        List<Object> tools = agentToolConfig.resolveSubAgentTools(getAgentTools(), AgentTypeEnum.PLANNER);
        AgentToolConfig.ToolBundle toolBundle = agentToolConfig.buildToolBundle(tools);
        ChatMemory memory = SubAgentMemoryFactory.createTemporary();
        String systemPrompt = renderSystemPrompt(PromptEnum.PLANNER, AgentTypeEnum.PLANNER, AgentModeEnum.AGENT, modelName);
        runtimeLog.info("subagent_prompt conversationId={} agentType={} modelName={} promptCode={} contentLength={} content=\n{}",
                conversationId,
                AgentTypeEnum.PLANNER.getCode(),
                modelName,
                PromptEnum.PLANNER.getCode(),
                systemPrompt.length(),
                systemPrompt);

        log.info("[SubAgentFactory] Building {} SubAgent, model={}, toolCount={}", AgentTypeEnum.PLANNER, modelName, tools.size());
        return AiServices.builder(PlannerAgentService.class)
                .streamingChatModel(model)
                .systemMessage(systemPrompt)
                .chatMemory(memory)
                .tools(toolBundle.executors(), toolBundle.immediateReturnToolNames())
                .build();
    }

    public MemoryWriterAgentService buildMemoryWriterAgent(String modelName, Long conversationId) {
        ChatModel model = resolveChatModel(modelName);
        List<Object> tools = agentToolConfig.resolveSubAgentTools(getAgentTools(), AgentTypeEnum.MEMORY_WRITER);
        AgentToolConfig.ToolBundle toolBundle = agentToolConfig.buildToolBundle(tools);
        ChatMemory memory = SubAgentMemoryFactory.createTemporary();
        String systemPrompt = renderSystemPrompt(PromptEnum.MEMORY_WRITER, AgentTypeEnum.MEMORY_WRITER, AgentModeEnum.AGENT, modelName);
        runtimeLog.info("subagent_prompt conversationId={} agentType={} modelName={} promptCode={} contentLength={} content=\n{}",
                conversationId,
                AgentTypeEnum.MEMORY_WRITER.getCode(),
                modelName,
                PromptEnum.MEMORY_WRITER.getCode(),
                systemPrompt.length(),
                systemPrompt);

        log.info("[SubAgentFactory] Building {} SubAgent, model={}, toolCount={}", AgentTypeEnum.MEMORY_WRITER, modelName, tools.size());
        return AiServices.builder(MemoryWriterAgentService.class)
                .chatModel(model)
                .systemMessage(systemPrompt)
                .chatMemory(memory)
                .tools(toolBundle.executors(), toolBundle.immediateReturnToolNames())
                .build();
    }

    private String renderSystemPrompt(PromptEnum promptEnum,
                                      AgentTypeEnum agentType,
                                      AgentModeEnum agentMode,
                                      String modelName) {
        return systemPromptManager.render(SystemPromptAssemblyContext.builder()
                        .promptEnum(promptEnum)
                        .agentType(agentType)
                        .agentMode(agentMode)
                        .modelName(modelName)
                        .language(promptEnum.getCode())
                        .availableSkills(agentSkillConfig.resolveAvailableSkills(agentType, agentMode))
                        .build())
                .renderedPrompt();
    }

    private StreamingChatModel resolveModel(String modelName) {
        StreamingChatModel model = modelsByName.get(modelName);
        if (model == null) {
            IllegalArgumentException ex = new IllegalArgumentException(
                    "No StreamingChatModel configured for model=" + modelName);
            log.error("[SubAgentFactory] Failed to resolve model for SubAgent", ex);
            throw ex;
        }
        return model;
    }

    private ChatModel resolveChatModel(String modelName) {
        ChatModel model = chatModelsByName.get(modelName);
        if (model == null) {
            IllegalArgumentException ex = new IllegalArgumentException(
                    "No ChatModel configured for model=" + modelName);
            log.error("[SubAgentFactory] Failed to resolve non-streaming model for SubAgent", ex);
            throw ex;
        }
        return model;
    }

    /**
     * Lazily fetch agent tools from ApplicationContext to avoid circular dependency at startup.
     */
    private List<Object> getAgentTools() {
        return new ArrayList<>(applicationContext.getBeansWithAnnotation(AgentTool.class).values());
    }
}
