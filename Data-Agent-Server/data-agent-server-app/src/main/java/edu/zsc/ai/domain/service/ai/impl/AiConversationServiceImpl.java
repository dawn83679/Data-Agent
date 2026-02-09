package edu.zsc.ai.domain.service.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.common.constant.ResponseCode;
import edu.zsc.ai.common.constant.ResponseMessageKey;
import edu.zsc.ai.domain.mapper.ai.AiConversationMapper;
import edu.zsc.ai.domain.model.dto.request.base.PageRequest;
import edu.zsc.ai.domain.model.entity.ai.AiCompressionRecord;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.domain.service.ai.AiCompressionRecordService;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.AiMessageService;
import edu.zsc.ai.domain.service.ai.AiTodoTaskService;
import edu.zsc.ai.util.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.dev33.satoken.stp.StpUtil;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationServiceImpl extends ServiceImpl<AiConversationMapper, AiConversation>
        implements AiConversationService {

    private final AiMessageService aiMessageService;
    private final AiCompressionRecordService aiCompressionRecordService;
    private final AiTodoTaskService aiTodoTaskService;

    private long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    public void checkAccess(Long userId, Long conversationId) {
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getId, conversationId)
                .eq(AiConversation::getUserId, userId);
        boolean exists = count(wrapper) > 0;

        BusinessException.assertTrue(exists, ResponseMessageKey.FORBIDDEN_MESSAGE);
    }

    @Override
    public AiConversation createConversation(Long userId, String title) {

        AiConversation conversation = AiConversation.builder()
                .userId(userId)
                .title(title)
                .tokenCount(0)
                .build();

        save(conversation);

        return conversation;
    }

    @Override
    public Page<AiConversation> pageByCurrentUser(PageRequest pageRequest) {
        long userId = getCurrentUserId();
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getUserId, userId)
                .orderByDesc(AiConversation::getUpdatedAt);
        Page<AiConversation> page = new Page<>(pageRequest.getCurrent(), pageRequest.getSize());
        return page(page, wrapper);
    }

    @Override
    public AiConversation getByIdForCurrentUser(Long conversationId) {
        long userId = getCurrentUserId();
        checkAccess(userId, conversationId);
        AiConversation one = getById(conversationId);
        BusinessException.assertNotNull(one, ResponseCode.FORBIDDEN, ResponseMessageKey.FORBIDDEN_MESSAGE);
        return one;
    }

    @Override
    public AiConversation updateTitle(Long conversationId, String title) {
        long userId = getCurrentUserId();
        checkAccess(userId, conversationId);
        LambdaUpdateWrapper<AiConversation> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiConversation::getId, conversationId)
                .set(AiConversation::getTitle, title)
                .set(AiConversation::getUpdatedAt, LocalDateTime.now());
        update(wrapper);
        return getById(conversationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByCurrentUser(Long conversationId) {
        long userId = getCurrentUserId();
        checkAccess(userId, conversationId);
        aiMessageService.removeAllByConversationId(conversationId);
        aiCompressionRecordService.remove(new LambdaQueryWrapper<AiCompressionRecord>()
                .eq(AiCompressionRecord::getConversationId, conversationId));
        aiTodoTaskService.removeByConversationId(conversationId);
        removeById(conversationId);
        log.info("Deleted conversation {} for user {}", conversationId, userId);
    }
}
