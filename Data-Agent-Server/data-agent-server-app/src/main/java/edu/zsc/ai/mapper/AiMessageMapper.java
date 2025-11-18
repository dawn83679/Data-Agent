package edu.zsc.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.zsc.ai.model.entity.AiMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper interface for ai_message table
 *
 * @author zgq
 */
@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessage> {
}