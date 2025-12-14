package edu.zsc.ai.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.zsc.ai.domain.model.entity.ai.AiMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper interface for ai_message table
 *
 * @author zgq
 */
@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessage> {
}