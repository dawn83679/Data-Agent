package edu.zsc.ai.service.impl;

import edu.zsc.ai.enums.ChatStatusEnum;
import edu.zsc.ai.model.dto.request.ChatRequest;
import edu.zsc.ai.model.dto.response.ChatResponse;
import edu.zsc.ai.service.ChatService;
import edu.zsc.ai.service.MessageQueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Decorator for ChatService that handles message queueing.
 *
 * @author zgq
 * @since 0.0.1
 */
@Slf4j
@Service("messageQueueChatService")
public class MessageQueueChatService implements ChatService {

    private final ChatService delegate;
    private final MessageQueueService messageQueueService;

    public MessageQueueChatService(@Qualifier("coreChatService") ChatService delegate,
            MessageQueueService messageQueueService) {
        this.delegate = delegate;
        this.messageQueueService = messageQueueService;
    }

    @Override
    public Flux<Object> sendMessage(ChatRequest request) {
        Long conversationId = request.getConversationId();
        String message = request.getMessage();

        // Check if the conversation is currently processing
        if (messageQueueService.isProcessing(conversationId)) {
            log.info("Conversation {} is busy, queuing message: {}", conversationId, message);
            messageQueueService.enqueue(conversationId, message);
            return Flux.just(ChatResponse.builder()
                    .conversationId(conversationId)
                    .status(ChatStatusEnum.QUEUED.name())
                    .build());
        }

        // If not busy, delegate to the core service
        return delegate.sendMessage(request);
    }
}
