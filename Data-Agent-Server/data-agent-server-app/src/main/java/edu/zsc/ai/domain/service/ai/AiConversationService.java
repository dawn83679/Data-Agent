package edu.zsc.ai.domain.service.ai;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.domain.model.dto.request.base.PageRequest;
import edu.zsc.ai.domain.model.dto.response.ai.ConversationMessageResponse;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.util.exception.BusinessException;

import java.util.List;

public interface AiConversationService extends IService<AiConversation> {

    /**
     * Verifies that the user has access to the conversation.
     *
     * @param userId          user ID
     * @param conversationId  conversation ID
     * @throws BusinessException if access is denied
     */
    void checkAccess(Long userId, Long conversationId);

    /**
     * Creates a new conversation for the user.
     *
     * @param userId user ID
     * @param title  conversation title (e.g. first N characters of the message)
     * @return the created conversation
     */
    AiConversation createConversation(Long userId, String title);

    /**
     * Returns a paginated list of conversations for the current user, ordered by updatedAt descending.
     * Current user ID is resolved from StpUtil in the service layer.
     */
    Page<AiConversation> pageByCurrentUser(PageRequest pageRequest);

    /**
     * Returns the conversation by ID for the current user. Throws if not found or access denied.
     */
    AiConversation getByIdForCurrentUser(Long conversationId);

    /**
     * Updates the conversation title for the current user.
     */
    AiConversation updateTitle(Long conversationId, String title);

    /**
     * Deletes the conversation for the current user, including all messages under it.
     */
    void deleteByCurrentUser(Long conversationId);

    /**
     * Returns history messages for the conversation. Verifies current user has access.
     */
    List<ConversationMessageResponse> getMessagesForCurrentUser(Long conversationId);

    /**
     * Adds token count to the conversation's total token usage.
     *
     * @param conversationId conversation ID
     * @param tokenCount     tokens to add
     */
    void addTokenCount(Long conversationId, Integer tokenCount);
}
