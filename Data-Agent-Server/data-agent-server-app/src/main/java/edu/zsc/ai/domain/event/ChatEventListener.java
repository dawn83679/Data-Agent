package edu.zsc.ai.domain.event;

import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.service.agent.SseEmitterRegistry;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.AiMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import reactor.core.publisher.Sinks;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatEventListener {

    private static final String STATUS_COMPRESSING = "compressing";

    private final SseEmitterRegistry sseEmitterRegistry;
    private final AiMessageService aiMessageService;
    private final AiConversationService aiConversationService;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void onCompressionStarted(MemoryCompressionStartedEvent event) {
        Long conversationId = event.getConversationId();
        sseEmitterRegistry.get(conversationId).ifPresent(sink -> {
            log.info("Emitting STATUS(compressing) for conversation {}", conversationId);
            sink.tryEmitNext(ChatResponseBlock.status(STATUS_COMPRESSING));
        });
    }

    @Async
    @EventListener
    public void onChatCompleted(ChatCompletedEvent event) {
        Long conversationId = event.getConversationId();
        Integer outputTokens = event.getOutputTokens();
        Integer totalTokens = event.getTotalTokens();

        log.info("Async processing chat completion for conversation {}: {} total tokens (output: {})",
                conversationId, totalTokens, outputTokens);

        if (outputTokens != null && outputTokens > 0) {
            aiMessageService.updateLastAiMessageTokenCount(conversationId, outputTokens);
        }

        aiConversationService.updateTokenCount(conversationId, totalTokens);
        eventPublisher.publishEvent(new ConversationMemoryAutoWriteRequestedEvent(this, conversationId));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConversationDeleted(ConversationDeletedEvent event) {
        Long conversationId = event.getConversationId();
        log.info("Async cleaning up messages for deleted conversation {}", conversationId);
        aiMessageService.removeByConversationId(conversationId);
    }
}
