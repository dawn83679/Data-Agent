package edu.zsc.ai.domain.service.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.domain.mapper.ai.AiMessageBlockMapper;
import edu.zsc.ai.domain.model.entity.ai.AiMessageBlock;
import edu.zsc.ai.domain.service.ai.AiMessageBlockService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiMessageBlockServiceImpl extends ServiceImpl<AiMessageBlockMapper, AiMessageBlock>
        implements AiMessageBlockService {

    @Override
    public List<AiMessageBlock> getByMessageId(Long messageId) {
        LambdaQueryWrapper<AiMessageBlock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMessageBlock::getMessageId, messageId)
                .orderByAsc(AiMessageBlock::getCreatedAt);
        return list(wrapper);
    }

    @Override
    public List<AiMessageBlock> getByMessageIds(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<AiMessageBlock> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(AiMessageBlock::getMessageId, messageIds)
                .orderByAsc(AiMessageBlock::getCreatedAt);
        return list(wrapper);
    }

    @Override
    public void saveBatchBlocks(List<AiMessageBlock> blocks) {
        saveBatch(blocks);
    }

    @Override
    public void deleteByMessageIds(List<Long> messageIds) {
        LambdaQueryWrapper<AiMessageBlock> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(AiMessageBlock::getMessageId, messageIds);
        remove(wrapper);
    }
}
