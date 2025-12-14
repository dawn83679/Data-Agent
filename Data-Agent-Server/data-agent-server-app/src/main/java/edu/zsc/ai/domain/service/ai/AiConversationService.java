package edu.zsc.ai.domain.service.ai;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.domain.model.dto.request.ai.conversation.CreateConversationRequest;
import edu.zsc.ai.domain.model.dto.request.ai.conversation.ConversationListRequest;
import edu.zsc.ai.domain.model.dto.request.ai.conversation.DeleteConversationRequest;
import edu.zsc.ai.domain.model.dto.request.ai.conversation.GetConversationRequest;
import edu.zsc.ai.domain.model.dto.request.ai.conversation.UpdateConversationRequest;
import edu.zsc.ai.domain.model.dto.response.ai.conversation.ConversationResponse;
import edu.zsc.ai.domain.model.dto.response.base.PageResponse;

/**
 * Service interface for ai_conversation operations
 *
 * @author zgq
 */
public interface AiConversationService extends IService<AiConversation> {

   
    ConversationResponse createConversation(CreateConversationRequest request);

    
    PageResponse<ConversationResponse> getConversationList(ConversationListRequest request);

  
    ConversationResponse getConversationById(GetConversationRequest request);

    
    ConversationResponse updateConversation(UpdateConversationRequest request);

    
    void deleteConversation(DeleteConversationRequest request);

    
    Long createOrGetConversation(Long conversationId, Long userId, String title);

   
    AiConversation getByIdAndUser(Long conversationId, Long userId);

   
    void updateConversationTokens(Long conversationId, Long userId, Integer tokenCount);
}