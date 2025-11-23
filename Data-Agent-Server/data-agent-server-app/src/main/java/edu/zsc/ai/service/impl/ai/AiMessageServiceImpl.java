package edu.zsc.ai.service.impl.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.converter.MessageConverter;
import edu.zsc.ai.enums.ai.message.MessageBlockTypeEnum;
import edu.zsc.ai.enums.ai.message.MessageRoleEnum;
import edu.zsc.ai.enums.ai.message.MessageStatusEnum;
import edu.zsc.ai.mapper.AiMessageMapper;
import edu.zsc.ai.model.dto.request.ai.message.MessageQueryRequest;
import edu.zsc.ai.model.dto.request.ai.message.SaveMessageRequest;
import edu.zsc.ai.model.dto.response.ai.message.HistoryContextResponse;
import edu.zsc.ai.model.dto.response.base.PageResponse;
import edu.zsc.ai.model.entity.ai.AiMessage;
import edu.zsc.ai.model.entity.ai.AiMessageBlock;
import edu.zsc.ai.service.ai.AiMessageBlockService;
import edu.zsc.ai.service.ai.AiMessageService;
import edu.zsc.ai.service.ai.AiConversationService;
import edu.zsc.ai.util.ConditionalUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiMessageServiceImpl extends ServiceImpl<AiMessageMapper, AiMessage>
        implements AiMessageService {

    private final AiMessageBlockService aiMessageBlockService;
    private final AiConversationService aiConversationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiMessage saveMessage(SaveMessageRequest request) {
        // 1. Save Message
        AiMessage message = new AiMessage();
        message.setConversationId(request.getConversationId());
        message.setRole(request.getRole());
        message.setTokenCount(request.getTokenCount());
        this.save(message);

        // 2. Save Block
        AiMessageBlock block = new AiMessageBlock();
        block.setMessageId(message.getId());
        ConditionalUtil.setIfNotBlankOrElse(request.getBlockType(), MessageBlockTypeEnum.TEXT.name(),
                block::setBlockType);
        block.setContent(request.getContent());
        block.setExtensionData(request.getExtensionData());
        aiMessageBlockService.save(block);

        return message;
    }

    @Override
    public HistoryContextResponse getAIContext(Long conversationId) {
        List<AiMessage> aiMessages = this.list(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId)
                .ne(AiMessage::getStatus, MessageStatusEnum.INVALID.getValue())
                .orderByDesc(AiMessage::getPriority)
                .orderByAsc(AiMessage::getId));

        if (aiMessages.isEmpty()) {
            return HistoryContextResponse.builder()
                    .messages(new ArrayList<>())
                    .totalTokenCount(0)
                    .build();
        }

        Map<Long, AiMessage> messageMap = aiMessages.stream()
                .collect(Collectors.toMap(AiMessage::getId, message -> message));

        List<Long> messageIds = new ArrayList<>(messageMap.keySet());

        List<AiMessageBlock> allBlocks = aiMessageBlockService.list(
                new LambdaQueryWrapper<AiMessageBlock>()
                        .in(AiMessageBlock::getMessageId, messageIds)
                        .orderByAsc(AiMessageBlock::getMessageId)
                        .orderByAsc(AiMessageBlock::getId));

        Map<Long, List<AiMessageBlock>> blocksByMessageId = new LinkedHashMap<>();
        for (AiMessage aiMessage : aiMessages) {
            blocksByMessageId.put(aiMessage.getId(), new ArrayList<>());
        }

        for (AiMessageBlock block : allBlocks) {
            blocksByMessageId.computeIfAbsent(block.getMessageId(), k -> new ArrayList<>())
                    .add(block);
        }

        List<Message> historyMessages = new ArrayList<>();
        int totalTokenCount = 0;

        for (Map.Entry<Long, List<AiMessageBlock>> entry : blocksByMessageId.entrySet()) {
            Long messageId = entry.getKey();
            List<AiMessageBlock> blocks = entry.getValue();
            AiMessage aiMessage = messageMap.get(messageId);

            boolean isToolResult = blocks.stream()
                    .anyMatch(block -> MessageBlockTypeEnum.TOOL_CALL_RESULT.name().equals(block.getBlockType()));

            String content = blocks.stream().map(AiMessageBlock::getContent).collect(Collectors.joining());

            String role;
            if (isToolResult) {
                role = MessageRoleEnum.USER.name();
            } else {
                role = aiMessage.getRole();
            }

            Message message = MessageConverter.toSpringMessage(role, content);
            historyMessages.add(message);

            // Accumulate total token count
            totalTokenCount += aiMessage.getTokenCount();
        }

        return HistoryContextResponse.builder()
                .messages(historyMessages)
                .totalTokenCount(totalTokenCount)
                .build();
    }

    @Override
    public PageResponse<AiMessage> getDisplayMessagesPaginated(MessageQueryRequest request) {
        LambdaQueryWrapper<AiMessage> queryWrapper = new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, request.getConversationId())
                .eq(AiMessage::getStatus, MessageStatusEnum.NORMAL.getValue());

        if (request.getCursorId() != null) {
            queryWrapper.lt(AiMessage::getId, request.getCursorId())
                    .orderByDesc(AiMessage::getId);
        } else {
            queryWrapper.orderByDesc(AiMessage::getId);
        }

        Page<AiMessage> page = new Page<>(request.getCurrent(), request.getSize());
        Page<AiMessage> result = this.page(page, queryWrapper);

        return PageResponse.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int rollbackMessages(Long conversationId, Long rollbackToMessageId) {
        Long userId = StpUtil.getLoginIdAsLong();

        // Verify conversation ownership
        aiConversationService.getByIdAndUser(conversationId, userId);

        AiMessage targetMessage = this.getById(rollbackToMessageId);
        if (targetMessage == null || !targetMessage.getConversationId().equals(conversationId)) {
            throw new IllegalArgumentException("Invalid rollback target message");
        }

        LambdaQueryWrapper<AiMessage> queryWrapper = new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId)
                .gt(AiMessage::getId, rollbackToMessageId)
                .ne(AiMessage::getStatus, MessageStatusEnum.INVALID.getValue());

        long count = this.count(queryWrapper);

        boolean success = this.lambdaUpdate()
                .eq(AiMessage::getConversationId, conversationId)
                .gt(AiMessage::getId, rollbackToMessageId)
                .ne(AiMessage::getStatus, MessageStatusEnum.INVALID.getValue())
                .set(AiMessage::getStatus, MessageStatusEnum.INVALID.getValue())
                .update();

        return success ? (int) count : 0;
    }
}