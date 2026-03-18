package edu.zsc.ai.config.ai;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.subagent.SubAgentMemoryFactory;
import edu.zsc.ai.agent.subagent.explorer.ExplorerAgentService;
import edu.zsc.ai.agent.subagent.planner.PlannerAgentService;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final Map<String, StreamingChatModel> modelsByName;
    private final AgentToolConfig agentToolConfig;
    private final ApplicationContext applicationContext;

    /**
     * Build an Explorer SubAgent using AiServices.builder().
     * Each invocation creates a fresh agent with isolated temporary memory.
     */
    public ExplorerAgentService buildExplorerAgent(String modelName, String systemPrompt) {
        StreamingChatModel model = resolveModel(modelName);
        List<Object> tools = agentToolConfig.resolveSubAgentTools(getAgentTools(), AgentTypeEnum.EXPLORER);
        ChatMemory memory = SubAgentMemoryFactory.createTemporary();

        log.info("[SubAgentFactory] Building {} SubAgent, model={}, toolCount={}", AgentTypeEnum.EXPLORER, modelName, tools.size());
        return AiServices.builder(ExplorerAgentService.class)
                .streamingChatModel(model)
                .systemMessage(systemPrompt)
                .chatMemory(memory)
                .tools(agentToolConfig.buildToolExecutors(tools))
                .build();
    }

    /**
     * Build a Planner SubAgent using AiServices.builder().
     * Each invocation creates a fresh agent with isolated temporary memory.
     */
    public PlannerAgentService buildPlannerAgent(String modelName, String systemPrompt) {
        StreamingChatModel model = resolveModel(modelName);
        List<Object> tools = agentToolConfig.resolveSubAgentTools(getAgentTools(), AgentTypeEnum.PLANNER);
        ChatMemory memory = SubAgentMemoryFactory.createTemporary();

        log.info("[SubAgentFactory] Building {} SubAgent, model={}, toolCount={}", AgentTypeEnum.PLANNER, modelName, tools.size());
        return AiServices.builder(PlannerAgentService.class)
                .streamingChatModel(model)
                .systemMessage(systemPrompt)
                .chatMemory(memory)
                .tools(agentToolConfig.buildToolExecutors(tools))
                .build();
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

    /**
     * Lazily fetch agent tools from ApplicationContext to avoid circular dependency at startup.
     */
    private List<Object> getAgentTools() {
        return new ArrayList<>(applicationContext.getBeansWithAnnotation(AgentTool.class).values());
    }
}
