package edu.zsc.ai.service.manager;

import edu.zsc.ai.model.entity.ai.AiConversation;

/**
 * 会话管理器，用于 TaskManager 在对话流程中统一调度会话的创建、校验及 token 统计。
 *
 * @author zgq
 */
public interface ConversationManager {

    /**
     * 创建或获取会话。
     *
     * @param conversationId 现有会话 ID，可为空
     * @param title          会话标题，可为空
     * @return 可用的会话 ID
     */
    Long createOrGetConversation(Long conversationId, String title);

    /**
     * 获取指定会话的当前 token 数。
     *
     * @param conversationId 会话 ID
     * @return 当前 token 数，默认 0
     */
    Integer getCurrentTokens(Long conversationId);

    /**
     * 更新会话的 token 统计。
     *
     * @param conversation 会话实体，需包含 ID 与 tokenCount
     */
    void updateConversationTokens(AiConversation conversation);
}

