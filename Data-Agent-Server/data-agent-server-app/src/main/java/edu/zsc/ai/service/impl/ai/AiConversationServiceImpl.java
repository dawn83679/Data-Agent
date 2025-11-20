package edu.zsc.ai.service.impl.ai;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.converter.ConversationConverter;
import edu.zsc.ai.mapper.AiConversationMapper;
import edu.zsc.ai.model.dto.request.ai.conversation.*;
import edu.zsc.ai.model.dto.response.ai.conversation.ConversationResponse;
import edu.zsc.ai.model.dto.response.base.PageResponse;
import edu.zsc.ai.model.entity.ai.AiConversation;
import edu.zsc.ai.service.AiConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service implementation for ai_conversation operations
 *
 * @author zgq
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationServiceImpl extends ServiceImpl<AiConversationMapper, AiConversation>
        implements AiConversationService {

    @Override
    public ConversationResponse createConversation(CreateConversationRequest request) {
        // Get current user ID from sa-token
        Long userId = StpUtil.getLoginIdAsLong();

        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(request.getTitle());

        this.save(conversation);

        log.info("Created conversation: id={}, userId={}, title={}", conversation.getId(), userId, request.getTitle());

        return ConversationConverter.toResponse(conversation);
    }

    @Override
    public PageResponse<ConversationResponse> getConversationList(ConversationListRequest request) {
        // Get current user ID from sa-token
        Long userId = StpUtil.getLoginIdAsLong();

        // Build query wrapper
        LambdaQueryWrapper<AiConversation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiConversation::getUserId, userId)
                   .orderByDesc(AiConversation::getUpdatedAt);

        // Add title filter if provided
        if (StringUtils.isNotBlank(request.getTitle())) {
            queryWrapper.like(AiConversation::getTitle, StringUtils.trim(request.getTitle()));
        }

        // Create page
        Page<AiConversation> page = new Page<>(request.getCurrent(), request.getSize());
        Page<AiConversation> result = this.page(page, queryWrapper);

        // Convert to response
        PageResponse<AiConversation> originalResponse = PageResponse.of(result);

        // Build response with converted records
        PageResponse<ConversationResponse> response = PageResponse.<ConversationResponse>builder()
                .current(originalResponse.getCurrent())
                .size(originalResponse.getSize())
                .total(originalResponse.getTotal())
                .pages(originalResponse.getPages())
                .records(ConversationConverter.toResponseList(result.getRecords()))
                .build();

        log.info("Got conversation list: userId={}, page={}, size={}, total={}",
                userId, request.getCurrent(), request.getSize(), response.getTotal());

        return response;
    }

    @Override
    public ConversationResponse getConversationById(GetConversationRequest request) {
        // Get current user ID from sa-token
        Long userId = StpUtil.getLoginIdAsLong();

        // Build query wrapper
        LambdaQueryWrapper<AiConversation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiConversation::getUserId, userId)
                   .eq(AiConversation::getId, request.getId());

        AiConversation conversation = this.getOne(queryWrapper);
        if (conversation == null) {
            log.warn("Conversation not found: userId={}, id={}", userId, request.getId());
            return null;
        }

        log.info("Got conversation by id: userId={}, id={}", userId, request.getId());
        return ConversationConverter.toResponse(conversation);
    }

    @Override
    public ConversationResponse updateConversation(UpdateConversationRequest request) {
        // Get current user ID from sa-token
        Long userId = StpUtil.getLoginIdAsLong();

        // Create update wrapper with conditions and set values
        LambdaUpdateWrapper<AiConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AiConversation::getUserId, userId)
                    .eq(AiConversation::getId, request.getId())
                    .set(AiConversation::getTitle, request.getTitle())
                    .set(AiConversation::getUpdatedAt, LocalDateTime.now());

        boolean updated = this.update(updateWrapper);
        if (!updated) {
            log.warn("Conversation not found for update: userId={}, id={}", userId, request.getId());
            return null;
        }

        // Get updated conversation for response
        AiConversation conversation = this.getById(request.getId());
        log.info("Updated conversation: userId={}, id={}, newTitle={}", userId, request.getId(), request.getTitle());
        return ConversationConverter.toResponse(conversation);
    }

    @Override
    public void deleteConversation(DeleteConversationRequest request) {
        // Get current user ID from sa-token
        Long userId = StpUtil.getLoginIdAsLong();

        // Create query wrapper for delete conditions
        LambdaQueryWrapper<AiConversation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiConversation::getUserId, userId)
                   .eq(AiConversation::getId, request.getId());

        boolean deleted = this.remove(queryWrapper);
        if (!deleted) {
            log.warn("Conversation not found for delete: userId={}, id={}", userId, request.getId());
            return;
        }

        log.info("Deleted conversation: userId={}, id={}", userId, request.getId());
    }

    @Override
    public Long createOrGetConversation(Long conversationId, Long userId, String title) {
        if (conversationId != null) {
            AiConversation existing = getByIdAndUser(conversationId, userId);
            if (existing != null) {
                return existing.getId();
            }
        }

        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        if (StringUtils.isNotBlank(title)) {
            conversation.setTitle(StringUtils.trim(title));
        }

        this.save(conversation);

        return conversation.getId();
    }

    @Override
    public AiConversation getByIdAndUser(Long conversationId, Long userId) {
        LambdaQueryWrapper<AiConversation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiConversation::getId, conversationId)
                .eq(AiConversation::getUserId, userId);
        return this.getOne(queryWrapper, false);
    }

    @Override
    public void updateConversationTokens(Long conversationId, Long userId, Integer tokenCount) {
        LambdaUpdateWrapper<AiConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AiConversation::getId, conversationId)
                .eq(AiConversation::getUserId, userId)
                .set(AiConversation::getTokenCount, tokenCount)
                .set(AiConversation::getUpdatedAt, LocalDateTime.now());
        this.update(updateWrapper);
    }
}