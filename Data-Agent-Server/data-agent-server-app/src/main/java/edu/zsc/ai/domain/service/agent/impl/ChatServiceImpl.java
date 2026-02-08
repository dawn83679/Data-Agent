package edu.zsc.ai.domain.service.agent.impl;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.TokenStream;
import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.common.enums.ai.MessageBlockEnum;
import edu.zsc.ai.domain.service.agent.ChatService;
import edu.zsc.ai.model.request.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ReActAgent reActAgent;

    @Override
    public Flux<ChatResponseBlock> chat(ChatRequest request) {
        Sinks.Many<ChatResponseBlock> sink = Sinks.many().unicast().onBackpressureBuffer();

        TokenStream tokenStream = reActAgent.chat(request);

        tokenStream.onPartialResponse(content -> {
            if (content != null && !content.isEmpty()) {
                ChatResponseBlock block = ChatResponseBlock.text(content);
                sink.tryEmitNext(block);
            }
        });

        tokenStream.onIntermediateResponse(response -> {
            if (response.aiMessage().hasToolExecutionRequests()) {
                for (ToolExecutionRequest toolRequest : response.aiMessage().toolExecutionRequests()) {
                    ChatResponseBlock block = ChatResponseBlock.builder()
                            .type(MessageBlockEnum.TOOL_CALL.name())
                            .toolName(toolRequest.name())
                            .toolArguments(toolRequest.arguments())
                            .done(false)
                            .build();
                    sink.tryEmitNext(block);
                }
            }
        });

        tokenStream.onToolExecuted(toolExecution -> {
            ChatResponseBlock block = ChatResponseBlock.builder()
                    .type(MessageBlockEnum.TOOL_RESULT.name())
                    .toolName(toolExecution.request().name())
                    .toolResult(toolExecution.result())
                    .done(false)
                    .build();
            sink.tryEmitNext(block);
        });

        tokenStream.onCompleteResponse(response -> {
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
