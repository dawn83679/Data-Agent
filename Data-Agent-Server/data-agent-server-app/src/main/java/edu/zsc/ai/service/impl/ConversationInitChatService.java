package edu.zsc.ai.service.impl;

import edu.zsc.ai.model.dto.request.ChatRequest;
import edu.zsc.ai.model.dto.request.CreateConversationRequest;
import edu.zsc.ai.model.dto.response.ConversationResponse;
import edu.zsc.ai.service.AiConversationService;
import edu.zsc.ai.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Decorator for ChatService that handles conversation initialization.
 * This is the primary entry point for ChatService injection.
 *
 * @author zgq
 * @since 0.0.1
 */
@Slf4j
@Service
@Primary
public class ConversationInitChatService implements ChatService {

    private final ChatService delegate;
    private final AiConversationService aiConversationService;

    public ConversationInitChatService(@Qualifier("messageQueueChatService") ChatService delegate,
            AiConversationService aiConversationService) {
        this.delegate = delegate;
        this.aiConversationService = aiConversationService;
    }

    @Override
    public Flux<Object> sendMessage(ChatRequest request) {
        Long conversationId = request.getConversationId();
        String message = request.getMessage();

        // If conversationId is null, create a new conversation
        if (conversationId == null) {
            log.info("No conversationId provided, creating new conversation");
            CreateConversationRequest createRequest = new CreateConversationRequest();
            createRequest.setTitle(message);
            ConversationResponse conversation = aiConversationService.createConversation(createRequest);
            conversationId = conversation.getId();
            request.setConversationId(conversationId);
            log.info("Created new conversation with ID: {}", conversationId);
        }

        // Delegate to the next service in the chain (MessageQueueChatService)
        return delegate.sendMessage(request);
    }
}
