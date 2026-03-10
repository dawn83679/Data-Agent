package edu.zsc.ai.config.ai;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import edu.zsc.ai.agent.MultiAgentWorker;
import edu.zsc.ai.agent.tool.ask.AskUserConfirmTool;
import edu.zsc.ai.agent.tool.memory.MemoryTool;
import edu.zsc.ai.agent.tool.skill.ActivateSkillTool;
import edu.zsc.ai.agent.tool.sql.DiscoveryTool;
import edu.zsc.ai.agent.tool.sql.ExecuteSqlTool;
import edu.zsc.ai.agent.tool.think.ThinkingTool;
import edu.zsc.ai.agent.tool.todo.TodoTool;
import edu.zsc.ai.common.enums.ai.AgentRoleEnum;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class MultiAgentWorkerConfig {

    private final Map<String, StreamingChatModel> modelsByName;
    private final MultiAgentPromptConfig multiAgentPromptConfig;
    private final DiscoveryTool discoveryTool;
    private final ThinkingTool thinkingTool;
    private final MemoryTool memoryTool;
    private final TodoTool todoTool;
    private final ActivateSkillTool activateSkillTool;
    private final ExecuteSqlTool executeSqlTool;
    private final AskUserConfirmTool askUserConfirmTool;

    public MultiAgentWorkerConfig(
            Map<String, StreamingChatModel> modelsByName,
            MultiAgentPromptConfig multiAgentPromptConfig,
            DiscoveryTool discoveryTool,
            ThinkingTool thinkingTool,
            MemoryTool memoryTool,
            TodoTool todoTool,
            ActivateSkillTool activateSkillTool,
            ExecuteSqlTool executeSqlTool,
            AskUserConfirmTool askUserConfirmTool) {
        this.modelsByName = modelsByName;
        this.multiAgentPromptConfig = multiAgentPromptConfig;
        this.discoveryTool = discoveryTool;
        this.thinkingTool = thinkingTool;
        this.memoryTool = memoryTool;
        this.todoTool = todoTool;
        this.activateSkillTool = activateSkillTool;
        this.executeSqlTool = executeSqlTool;
        this.askUserConfirmTool = askUserConfirmTool;
    }

    @Bean("schemaAnalystWorkers")
    public Map<String, MultiAgentWorker> schemaAnalystWorkers() {
        return buildWorkers(AgentRoleEnum.SCHEMA_ANALYST);
    }

    @Bean("sqlPlannerWorkers")
    public Map<String, MultiAgentWorker> sqlPlannerWorkers() {
        return buildWorkers(AgentRoleEnum.SQL_PLANNER);
    }

    @Bean("sqlExecutorWorkers")
    public Map<String, MultiAgentWorker> sqlExecutorWorkers() {
        return buildWorkers(AgentRoleEnum.SQL_EXECUTOR);
    }

    @Bean("resultAnalystWorkers")
    public Map<String, MultiAgentWorker> resultAnalystWorkers() {
        return buildWorkers(AgentRoleEnum.RESULT_ANALYST);
    }

    private Map<String, MultiAgentWorker> buildWorkers(AgentRoleEnum role) {
        Map<String, MultiAgentWorker> workers = new LinkedHashMap<>();
        List<Object> roleTools = roleTools(role);

        for (Map.Entry<String, StreamingChatModel> entry : modelsByName.entrySet()) {
            String modelName = entry.getKey();
            StreamingChatModel model = entry.getValue();
            String key = workerKey(modelName);
            workers.put(key, AiServices.builder(MultiAgentWorker.class)
                    .streamingChatModel(model)
                    .systemMessage(multiAgentPromptConfig.getPrompt(role, "en"))
                    .tools(roleTools)
                    .build());
        }

        return Collections.unmodifiableMap(workers);
    }

    private List<Object> roleTools(AgentRoleEnum role) {
        return switch (role) {
            case SCHEMA_ANALYST -> List.of(discoveryTool, thinkingTool, memoryTool);
            case SQL_PLANNER -> List.of(thinkingTool, memoryTool, todoTool, activateSkillTool);
            case SQL_EXECUTOR -> List.of(executeSqlTool, askUserConfirmTool);
            case RESULT_ANALYST -> List.of();
            default -> List.of();
        };
    }

    public static String workerKey(String modelName) {
        return modelName;
    }
}
