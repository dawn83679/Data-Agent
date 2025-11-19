package edu.zsc.ai.service;

import edu.zsc.ai.model.dto.request.ChatRequest;

import reactor.core.publisher.Flux;

/**
 * Service interface for chat operations.
 *
 * @author zgq
 * @since 0.0.1
 */
public interface ChatService {

    /**
     * Send a message to the AI.
     * Handles message queueing if the conversation is busy.
     *
     * @param request chat request
     * @return chat response stream
     */
    Flux<Object> sendMessage(ChatRequest request);
}
