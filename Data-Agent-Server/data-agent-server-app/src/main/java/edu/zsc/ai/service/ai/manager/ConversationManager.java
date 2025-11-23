package edu.zsc.ai.service.ai.manager;

import edu.zsc.ai.model.entity.ai.AiConversation;

/**
 * Conversation manager, used by TaskManager to coordinate conversation creation, validation and token statistics in conversation flow.
 *
 * @author zgq
 */
public interface ConversationManager {

    /**
     * Create or get conversation.
     *
     * @param conversationId existing conversation ID, can be null
     * @param title conversation title, can be null
     * @return available conversation ID
     */
    Long createOrGetConversation(Long conversationId, String title);

    /**
     * Get current token count for specified conversation.
     *
     * @param conversationId conversation ID
     * @return current token count, default 0
     */
    Integer getCurrentTokens(Long conversationId);

    /**
     * Update conversation token statistics.
     *
     * @param conversation conversation entity, must include ID and tokenCount
     */
    void updateConversationTokens(AiConversation conversation);
}

