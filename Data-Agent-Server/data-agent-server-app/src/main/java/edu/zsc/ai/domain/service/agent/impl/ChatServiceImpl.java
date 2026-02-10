package edu.zsc.ai.domain.service.agent.impl;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.agent.ReActAgentProvider;
import edu.zsc.ai.common.constant.HitlConstants;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.domain.service.agent.ChatService;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.model.request.ChatRequest;
import edu.zsc.ai.model.request.SubmitToolAnswerRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String DEFAULT_MODEL = ModelEnum.QWEN3_MAX.getModelName();

    private final ReActAgentProvider reActAgentProvider;
    private final AiConversationService aiConversationService;
    private final ChatMemoryStore chatMemoryStore;

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
        streamTokenStreamToSink(tokenStream, sink, request.getConversationId());
        return sink.asFlux();
    }

    @Override
    public Flux<ChatResponseBlock> submitToolAnswerAndContinue(SubmitToolAnswerRequest request) {
        String modelName = StringUtils.isNotBlank(request.getModel())
                ? request.getModel().trim()
                : DEFAULT_MODEL;
        try {
            ModelEnum.fromModelName(modelName);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown model: " + modelName, e);
        }

        if (request.getConversationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversationId is required");
        }

        ReActAgent agent = reActAgentProvider.getAgent(modelName);

        Long userId = RequestContext.getUserId();
        String memoryId = userId + ":" + request.getConversationId();

        List<ChatMessage> messages = chatMemoryStore.getMessages(memoryId);
        if (messages == null || messages.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No messages for this conversation");
        }

        String toolCallId = request.getToolCallId();
        String answer = request.getAnswer();
        boolean replaced = false;
        List<ChatMessage> newMessages = new ArrayList<>(messages);

        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if (msg instanceof ToolExecutionResultMessage toolMsg) {
                if (HitlConstants.ASK_USER_QUESTION_TOOL_NAME.equals(toolMsg.toolName())
                        && toolCallId.equals(toolMsg.id())) {
                    ToolExecutionResultMessage replacedMsg = ToolExecutionResultMessage.from(
                            toolMsg.id(), toolMsg.toolName(), answer);
                    newMessages.set(i, replacedMsg);
                    replaced = true;
                    break;
                }
            }
        }

        if (!replaced) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No matching askUserQuestion tool result for id: " + toolCallId);
        }

        chatMemoryStore.updateMessages(memoryId, newMessages);

        Sinks.Many<ChatResponseBlock> sink = Sinks.many().unicast().onBackpressureBuffer();
        InvocationParameters parameters = InvocationParameters.from(RequestContext.toMap());
        TokenStream tokenStream = agent.chat(memoryId, HitlConstants.HITL_CONTINUE_MESSAGE, parameters);
        streamTokenStreamToSink(tokenStream, sink, request.getConversationId());
        return sink.asFlux();
    }

    private void streamTokenStreamToSink(TokenStream tokenStream,
                                         Sinks.Many<ChatResponseBlock> sink,
                                         Long conversationId) {
        tokenStream.onPartialResponse(content -> {
            if (StringUtils.isNotBlank(content)) {
                sink.tryEmitNext(ChatResponseBlock.text(content));
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
            sink.tryEmitNext(ChatResponseBlock.doneBlock(conversationId));
            sink.tryEmitComplete();
        });

        tokenStream.onError(error -> {
            log.error("Error in chat stream", error);
            sink.tryEmitError(error);
        });

        tokenStream.start();
    }
}
