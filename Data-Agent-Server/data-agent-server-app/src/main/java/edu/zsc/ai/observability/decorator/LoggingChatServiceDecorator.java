package edu.zsc.ai.observability.decorator;

import edu.zsc.ai.api.model.request.ChatRequest;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.service.agent.ChatService;
import edu.zsc.ai.domain.service.agent.impl.ChatServiceImpl;
import edu.zsc.ai.observability.AgentLogEvent;
import edu.zsc.ai.observability.AgentLogService;
import edu.zsc.ai.observability.AgentLogType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Primary
@RequiredArgsConstructor
public class LoggingChatServiceDecorator implements ChatService {

    private final ChatServiceImpl delegate;
    private final AgentLogService agentLogService;

    @Override
    public Flux<ChatResponseBlock> chat(ChatRequest request) {
        AtomicReference<Long> conversationIdRef = new AtomicReference<>(request != null ? request.getConversationId() : null);
        AtomicBoolean started = new AtomicBoolean(false);
        return delegate.chat(request)
                .doOnNext(block -> {
                    if (block != null && block.getConversationId() != null) {
                        conversationIdRef.set(block.getConversationId());
                    }
                    if (started.compareAndSet(false, true)) {
                        agentLogService.record(AgentLogEvent.builder()
                                .timestamp(Instant.now())
                                .type(AgentLogType.CONVERSATION_START)
                                .loggerName("ChatService")
                                .conversationId(conversationIdRef.get())
                                .message("conversation_start")
                                .build());
                    }
                    agentLogService.recordChatBlock(conversationIdRef.get(), block);
                })
                .doOnComplete(() -> agentLogService.record(AgentLogEvent.builder()
                        .timestamp(Instant.now())
                        .type(AgentLogType.CONVERSATION_COMPLETE)
                        .loggerName("ChatService")
                        .conversationId(conversationIdRef.get())
                        .message("conversation_complete")
                        .build()))
                .doOnError(error -> agentLogService.recordError(
                        AgentLogType.CONVERSATION_ERROR,
                        "ChatService",
                        "conversation_error",
                        error,
                        null));
    }
}
