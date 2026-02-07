package edu.zsc.ai.domain.service.agent.impl;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import edu.zsc.ai.agent.SqlAgent;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlockType;
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

    private final SqlAgent sqlAgent;

    @Override
    public Flux<ChatResponseBlock> chat(ChatRequest request) {
        Sinks.Many<ChatResponseBlock> sink = Sinks.many().unicast().onBackpressureBuffer();

        TokenStream tokenStream = sqlAgent.chat(request);

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
                            .type(ChatResponseBlockType.TOOL_CALL.name())
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
                    .type(ChatResponseBlockType.TOOL_RESULT.name())
                    .toolName(toolExecution.request().name())
                    .toolResult(toolExecution.result())
                    .done(false)
                    .build();
            sink.tryEmitNext(block);
        });

        tokenStream.onCompleteResponse(response -> {
            sink.tryEmitNext(ChatResponseBlock.done());
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
