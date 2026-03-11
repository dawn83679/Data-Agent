package edu.zsc.ai.config.ai;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import edu.zsc.ai.agent.MultiAgentWorker;
import edu.zsc.ai.agent.tool.ask.AskUserConfirmTool;
import edu.zsc.ai.agent.tool.sql.DiscoveryTool;
import edu.zsc.ai.agent.tool.sql.SchemaDetailTool;
import edu.zsc.ai.agent.tool.sql.SelectSqlTool;
import edu.zsc.ai.agent.tool.sql.WriteSqlTool;
import edu.zsc.ai.agent.tool.think.ThinkingTool;
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
    private final SelectSqlTool selectSqlTool;
    private final WriteSqlTool writeSqlTool;
    private final SchemaDetailTool schemaDetailTool;
    private final AskUserConfirmTool askUserConfirmTool;

    public MultiAgentWorkerConfig(
            Map<String, StreamingChatModel> modelsByName,
            MultiAgentPromptConfig multiAgentPromptConfig,
            DiscoveryTool discoveryTool,
            ThinkingTool thinkingTool,
            SelectSqlTool selectSqlTool,
            WriteSqlTool writeSqlTool,
            SchemaDetailTool schemaDetailTool,
            AskUserConfirmTool askUserConfirmTool) {
        this.modelsByName = modelsByName;
        this.multiAgentPromptConfig = multiAgentPromptConfig;
        this.discoveryTool = discoveryTool;
        this.thinkingTool = thinkingTool;
        this.selectSqlTool = selectSqlTool;
        this.writeSqlTool = writeSqlTool;
        this.schemaDetailTool = schemaDetailTool;
        this.askUserConfirmTool = askUserConfirmTool;
    }

    @Bean("schemaExplorerWorkers")
    public Map<String, MultiAgentWorker> schemaExplorerWorkers() {
        return buildWorkers(AgentRoleEnum.SCHEMA_EXPLORER);
    }

    @Bean("dataAnalystWorkers")
    public Map<String, MultiAgentWorker> dataAnalystWorkers() {
        return buildWorkers(AgentRoleEnum.DATA_ANALYST);
    }

    @Bean("dataWriterWorkers")
    public Map<String, MultiAgentWorker> dataWriterWorkers() {
        return buildWorkers(AgentRoleEnum.DATA_WRITER);
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
            case SCHEMA_EXPLORER -> List.of(discoveryTool, schemaDetailTool, thinkingTool);
            case DATA_ANALYST -> List.of(selectSqlTool, thinkingTool);
            case DATA_WRITER -> List.of(schemaDetailTool, writeSqlTool, askUserConfirmTool, thinkingTool);
            default -> List.of();
        };
    }

    public static String workerKey(String modelName) {
        return modelName;
    }
}
