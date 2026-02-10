package edu.zsc.ai.domain.service.agent.impl;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.TokenStream;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.agent.ReActAgentProvider;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.domain.service.agent.ChatService;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.model.request.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String DEFAULT_MODEL = ModelEnum.QWEN3_MAX.getModelName();

    private final ReActAgentProvider reActAgentProvider;
    private final AiConversationService aiConversationService;

    @Override
    public Flux<ChatResponseBlock> chat(ChatRequest request) {
        String modelName = StringUtils.isNotBlank(request.getModel()) ? request.getModel().trim() : DEFAULT_MODEL;
        try {
            ModelEnum.fromModelName(modelName);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown model: " + modelName, e);
        }

        ReActAgent agent = reActAgentProvider.getAgent(modelName);

        if (request.getConversationId() == null) {
            Long userId = RequestContext.getUserId();
            AiConversation conversation = aiConversationService.createConversation(userId, request.getMessage());
            request.setConversationId(conversation.getId());
            RequestContext.updateConversationId(conversation.getId());
            log.info("Created new conversation: id={}", conversation.getId());
        }

        Sinks.Many<ChatResponseBlock> sink = Sinks.many().unicast().onBackpressureBuffer();

        String memoryId = RequestContext.getUserId() + ":" + request.getConversationId();

        InvocationParameters parameters = InvocationParameters.from(RequestContext.toMap());
        TokenStream tokenStream = agent.chat(memoryId, request.getMessage(), parameters);

        tokenStream.onPartialResponse(content -> {
            if (StringUtils.isNotBlank(content)) {
                ChatResponseBlock block = ChatResponseBlock.text(content);
                sink.tryEmitNext(block);
            }
        });

        tokenStream.onPartialThinking(partial -> {
            if (StringUtils.isNotBlank(partial.text())) {
                sink.tryEmitNext(ChatResponseBlock.thought(partial.text()));
            }
        });

        tokenStream.onIntermediateResponse(response -> {
            if (response.aiMessage().hasToolExecutionRequests()) {
                for (ToolExecutionRequest toolRequest : response.aiMessage().toolExecutionRequests()) {
                    sink.tryEmitNext(ChatResponseBlock.toolCall(
                            toolRequest.id(),
                            toolRequest.name(),
                            toolRequest.arguments()));
                }
            }
        });

        tokenStream.onToolExecuted(toolExecution -> {
            ToolExecutionRequest req = toolExecution.request();
            sink.tryEmitNext(ChatResponseBlock.toolResult(
                    req.id(),
                    req.name(),
                    toolExecution.result(),
                    toolExecution.hasFailed()));
        });

        tokenStream.onCompleteResponse(response -> {
            sink.tryEmitNext(ChatResponseBlock.doneBlock(request.getConversationId()));
            sink.tryEmitComplete();
        });

        tokenStream.onError(error -> {
            log.error("Error in chat stream", error);
            sink.tryEmitError(error);
        });

        tokenStream.start();

        return sink.asFlux();
    }
}
