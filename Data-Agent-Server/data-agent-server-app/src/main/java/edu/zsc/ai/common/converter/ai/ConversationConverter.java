package edu.zsc.ai.common.converter.ai;

import edu.zsc.ai.domain.model.dto.response.ai.conversation.ConversationResponse;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converter for conversation entity and response DTO
 *
 * @author zgq
 */
public class ConversationConverter {

    /**
     * Convert conversation entity to response DTO
     *
     * @param conversation conversation entity
     * @return conversation response DTO
     */
    public static ConversationResponse toResponse(AiConversation conversation) {
        if (conversation == null) {
            return null;
        }

        ConversationResponse response = new ConversationResponse();
        response.setId(conversation.getId());
        response.setUserId(conversation.getUserId());
        response.setTitle(conversation.getTitle());
        response.setTokenCount(conversation.getTokenCount());
        response.setCreatedAt(conversation.getCreatedAt());
        response.setUpdatedAt(conversation.getUpdatedAt());

        return response;
    }

    /**
     * Convert conversation entity list to response DTO list
     *
     * @param conversations conversation entity list
     * @return conversation response DTO list
     */
    public static List<ConversationResponse> toResponseList(List<AiConversation> conversations) {
        if (conversations == null || conversations.isEmpty()) {
            return Collections.emptyList();
        }

        return conversations.stream()
                .map(ConversationConverter::toResponse)
                .collect(Collectors.toList());
    }
}