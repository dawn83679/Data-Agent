package edu.zsc.ai.domain.service.ai;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.domain.model.entity.ai.AiTodoTask;

/**
 * AiTodoTaskService
 * Service interface for AiTodoTask.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
public interface AiTodoTaskService extends IService<AiTodoTask> {

    /**
     * Get todo task by conversation ID.
     * Checks cache first, then database.
     *
     * @param conversationId conversation ID
     * @return AiTodoTask entity
     */
    AiTodoTask getByConversationId(Long conversationId);

    /**
     * Save todo task by conversation ID.
     * Saves to database and updates cache.
     *
     * @param task AiTodoTask entity
     * @return true if success
     */
    boolean saveByConversationId(AiTodoTask task);

    /**
     * Update todo task by conversation ID.
     * Updates database and updates cache.
     *
     * @param task AiTodoTask entity
     * @return true if success
     */
    boolean updateByConversationId(AiTodoTask task);

    /**
     * Remove todo task by conversation ID.
     * Removes from database and cache.
     *
     * @param conversationId conversation ID
     * @return true if success
     */
    boolean removeByConversationId(Long conversationId);
}
