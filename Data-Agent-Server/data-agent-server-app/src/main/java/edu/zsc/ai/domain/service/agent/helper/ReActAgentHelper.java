package edu.zsc.ai.domain.service.agent.helper;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.common.constant.ChatErrorConstants;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import edu.zsc.ai.domain.service.agent.AgentSystemPromptService;
import edu.zsc.ai.domain.service.agent.model.AgentChatCommand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Helper for model validation and dynamic ReActAgent construction.
 */
@Component
public class ReActAgentHelper {

    private static final String DEFAULT_MODEL = ModelEnum.QWEN3_MAX.getModelName();

    private final ChatMemoryProvider chatMemoryProvider;
    private final List<Object> agentTools;
    private final AgentSystemPromptService agentSystemPromptService;
    private final StreamingChatModel streamingChatModelQwen3Max;
    private final StreamingChatModel streamingChatModelQwen3MaxThinking;
    private final StreamingChatModel streamingChatModelQwenPlus;
    private final McpToolProvider mcpToolProvider;

    public ReActAgentHelper(
            ChatMemoryProvider chatMemoryProvider,
            @Qualifier("agentTools") List<Object> agentTools,
            AgentSystemPromptService agentSystemPromptService,
            @Qualifier("streamingChatModelQwen3Max") StreamingChatModel streamingChatModelQwen3Max,
            @Qualifier("streamingChatModelQwen3MaxThinking") StreamingChatModel streamingChatModelQwen3MaxThinking,
            @Qualifier("streamingChatModelQwenPlus") StreamingChatModel streamingChatModelQwenPlus,
            @Qualifier("mcpToolProvider") McpToolProvider mcpToolProvider) {
        this.chatMemoryProvider = chatMemoryProvider;
        this.agentTools = agentTools;
        this.agentSystemPromptService = agentSystemPromptService;
        this.streamingChatModelQwen3Max = streamingChatModelQwen3Max;
        this.streamingChatModelQwen3MaxThinking = streamingChatModelQwen3MaxThinking;
        this.streamingChatModelQwenPlus = streamingChatModelQwenPlus;
        this.mcpToolProvider = mcpToolProvider;
    }

    /**
     * Resolves request model to a valid model name, or DEFAULT_MODEL if blank.
     * Throws ResponseStatusException if the model is not supported.
     */
    public String validateAndResolveModel(String requestModel) {
        String modelName = StringUtils.isNotBlank(requestModel) ? requestModel.trim() : DEFAULT_MODEL;
        try {
            ModelEnum.fromModelName(modelName);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    ChatErrorConstants.UNKNOWN_MODEL_PREFIX + modelName, e);
        }
        return modelName;
    }

    public ReActAgent buildAgent(String modelName, String acceptLanguage) {
        StreamingChatModel streamingChatModel = resolveStreamingModel(modelName);
        return AiServices.builder(ReActAgent.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(agentTools)
                .toolProvider(mcpToolProvider)
                .systemMessage(agentSystemPromptService.resolvePrompt(acceptLanguage))
                .build();
    }

    public TokenStream startChat(ReActAgent agent, AgentChatCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("AgentChatCommand must not be null");
        }
        if (agent == null) {
            throw new IllegalArgumentException("ReActAgent must not be null");
        }
        if (StringUtils.isBlank(command.getMemoryId())) {
            throw new IllegalArgumentException("memoryId must not be blank");
        }
        if (StringUtils.isBlank(command.getText())) {
            throw new IllegalArgumentException("text must not be blank");
        }
        if (command.getParameters() == null) {
            throw new IllegalArgumentException("parameters must not be null");
        }
        if (command.getImageUrls() != null && !command.getImageUrls().isEmpty()) {
            throw new UnsupportedOperationException("Multimodal image input is not supported yet");
        }
        return agent.chat(command.getMemoryId(), command.getText(), command.getParameters());
    }

    private StreamingChatModel resolveStreamingModel(String modelName) {
        if (ModelEnum.QWEN3_MAX.getModelName().equalsIgnoreCase(modelName)) {
            return streamingChatModelQwen3Max;
        }
        if (ModelEnum.QWEN3_MAX_THINKING.getModelName().equalsIgnoreCase(modelName)) {
            return streamingChatModelQwen3MaxThinking;
        }
        if (ModelEnum.QWEN_PLUS.getModelName().equalsIgnoreCase(modelName)) {
            return streamingChatModelQwenPlus;
        }
        throw new IllegalArgumentException("No StreamingChatModel configured for model: " + modelName);
    }
}
