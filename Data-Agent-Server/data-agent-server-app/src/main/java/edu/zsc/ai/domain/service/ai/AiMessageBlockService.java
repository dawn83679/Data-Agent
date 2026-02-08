package edu.zsc.ai.domain.service.ai;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.domain.model.entity.ai.AiMessageBlock;

import java.util.List;

public interface AiMessageBlockService extends IService<AiMessageBlock> {


    List<AiMessageBlock> getByMessageId(Long messageId);


    List<AiMessageBlock> getByMessageIds(List<Long> messageIds);


    void saveBatchBlocks(List<AiMessageBlock> blocks);


    void deleteByMessageIds(List<Long> messageIds);
}
