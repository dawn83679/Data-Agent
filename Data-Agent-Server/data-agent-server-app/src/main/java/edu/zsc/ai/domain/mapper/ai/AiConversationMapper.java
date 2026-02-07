package edu.zsc.ai.domain.mapper.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiConversationMapper extends BaseMapper<AiConversation> {
}
