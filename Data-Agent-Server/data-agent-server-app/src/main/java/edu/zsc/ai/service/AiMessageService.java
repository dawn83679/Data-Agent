package edu.zsc.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.model.dto.request.ai.message.MessageQueryRequest;
import edu.zsc.ai.model.dto.request.ai.message.SaveMessageRequest;
import edu.zsc.ai.model.dto.response.ai.message.HistoryContextResponse;
import edu.zsc.ai.model.dto.response.base.PageResponse;
import edu.zsc.ai.model.entity.ai.AiMessage;

/**
 * Service interface for ai_message operations
 *
 * @author zgq
 */
public interface AiMessageService extends IService<AiMessage> {

    /**
     * Save a message
     *
     * @param request save message request
     * @return saved message entity
     */
    AiMessage saveMessage(SaveMessageRequest request);  

    /**
     * Get messages for AI context (includes compressed messages, excludes invalid)
     * Ordered by priority DESC, id ASC
     *
     * @param conversationId conversation ID
     * @return history context response with messages and token information
     */
    HistoryContextResponse getAIContext(Long conversationId);

    
    /**
     * Get paginated messages for display with filtering and sorting
     * Supports both traditional pagination and cursor-based pagination
     *
     * @param request message query request with pagination parameters
     * @return paginated response containing message entities
     */
    PageResponse<AiMessage> getDisplayMessagesPaginated(MessageQueryRequest request);

    }