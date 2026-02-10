package edu.zsc.ai.common.converter.ai;

import edu.zsc.ai.domain.model.dto.response.ai.ConversationResponse;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import org.springframework.beans.BeanUtils;

/**
 * Converter for AI conversation entity and DTOs.
 */
public final class ConversationConverter {

    private ConversationConverter() {
    }

    public static ConversationResponse toResponse(AiConversation entity) {
        if (entity == null) {
            return null;
        }
        ConversationResponse response = new ConversationResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }
}
