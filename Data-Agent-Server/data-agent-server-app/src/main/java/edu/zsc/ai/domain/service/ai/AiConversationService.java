package edu.zsc.ai.domain.service.ai;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;

public interface AiConversationService extends IService<AiConversation> {

    /**
     * 检查用户是否有权限访问该会话
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @throws SecurityException 如果无权限
     */
    void checkAccess(Long userId, Long conversationId);
}
