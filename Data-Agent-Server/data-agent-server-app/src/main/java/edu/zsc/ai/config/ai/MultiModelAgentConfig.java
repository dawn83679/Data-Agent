package edu.zsc.ai.config.ai;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import dev.langchain4j.community.model.dashscope.QwenChatRequestParameters;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.agent.ReActAgentProvider;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import edu.zsc.ai.agent.tool.AskUserConfirmTool;
import edu.zsc.ai.agent.tool.AskUserQuestionTool;
import edu.zsc.ai.agent.tool.ConnectionTool;
import edu.zsc.ai.agent.tool.DatabaseTool;
import edu.zsc.ai.agent.tool.ExecuteSqlTool;
import edu.zsc.ai.agent.tool.FunctionTool;
import edu.zsc.ai.agent.tool.IndexTool;
import edu.zsc.ai.agent.tool.ProcedureTool;
import edu.zsc.ai.agent.tool.TableTool;
import edu.zsc.ai.agent.tool.TodoTool;
import edu.zsc.ai.agent.tool.TriggerTool;
import edu.zsc.ai.agent.tool.ViewTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configures multiple StreamingChatModel and ReActAgent beans per supported model (ModelEnum),
 * and provides ReActAgentProvider for runtime selection by request model name.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class MultiModelAgentConfig {

    private static final int THINKING_BUDGET = 1000;

    @Value("${langchain4j.community.dashscope.streaming-chat-model.api-key}")
    private String apiKey;

    @Bean("streamingChatModelQwen3Max")
    public StreamingChatModel streamingChatModelQwen3Max() {
        return QwenStreamingChatModel.builder()
                .apiKey(apiKey)
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
                .apiKey(apiKey)
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
                .apiKey(apiKey)
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
            AskUserConfirmTool askUserConfirmTool,
            ConnectionTool connectionTool,
            DatabaseTool databaseTool,
            ExecuteSqlTool executeSqlTool,
            ViewTool viewTool,
            FunctionTool functionTool,
            ProcedureTool procedureTool,
            TriggerTool triggerTool,
            IndexTool indexTool,
            @Qualifier("mcpToolProvider") McpToolProvider mcpToolProvider) {
        return AiServices.builder(ReActAgent.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(todoTool, tableTool, askUserQuestionTool, askUserConfirmTool,
                       connectionTool, databaseTool, executeSqlTool,
                       viewTool, functionTool, procedureTool, triggerTool, indexTool)
                .toolProvider(mcpToolProvider)
                .build();
    }

    @Bean("reActAgentQwen3MaxThinking")
    public ReActAgent reActAgentQwen3MaxThinking(
            @Qualifier("streamingChatModelQwen3MaxThinking") StreamingChatModel streamingChatModel,
            ChatMemoryProvider chatMemoryProvider,
            TodoTool todoTool,
            TableTool tableTool,
            AskUserQuestionTool askUserQuestionTool,
            AskUserConfirmTool askUserConfirmTool,
            ConnectionTool connectionTool,
            DatabaseTool databaseTool,
            ExecuteSqlTool executeSqlTool,
            ViewTool viewTool,
            FunctionTool functionTool,
            ProcedureTool procedureTool,
            TriggerTool triggerTool,
            IndexTool indexTool,
            @Qualifier("mcpToolProvider") McpToolProvider mcpToolProvider) {
        return AiServices.builder(ReActAgent.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(todoTool, tableTool, askUserQuestionTool, askUserConfirmTool,
                       connectionTool, databaseTool, executeSqlTool,
                       viewTool, functionTool, procedureTool, triggerTool, indexTool)
                .toolProvider(mcpToolProvider)
                .build();
    }

    @Bean("reActAgentQwenPlus")
    public ReActAgent reActAgentQwenPlus(
            @Qualifier("streamingChatModelQwenPlus") StreamingChatModel streamingChatModel,
            ChatMemoryProvider chatMemoryProvider,
            TodoTool todoTool,
            TableTool tableTool,
            AskUserQuestionTool askUserQuestionTool,
            AskUserConfirmTool askUserConfirmTool,
            ConnectionTool connectionTool,
            DatabaseTool databaseTool,
            ExecuteSqlTool executeSqlTool,
            ViewTool viewTool,
            FunctionTool functionTool,
            ProcedureTool procedureTool,
            TriggerTool triggerTool,
            IndexTool indexTool,
            @Qualifier("mcpToolProvider") McpToolProvider mcpToolProvider) {
        return AiServices.builder(ReActAgent.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(todoTool, tableTool, askUserQuestionTool, askUserConfirmTool,
                       connectionTool, databaseTool, executeSqlTool,
                       viewTool, functionTool, procedureTool, triggerTool, indexTool)
                .toolProvider(mcpToolProvider)
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
