package edu.zsc.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.zsc.ai.model.entity.ai.AiConversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper interface for ai_conversation table
 *
 * @author zgq
 */
@Mapper
public interface AiConversationMapper extends BaseMapper<AiConversation> {
}