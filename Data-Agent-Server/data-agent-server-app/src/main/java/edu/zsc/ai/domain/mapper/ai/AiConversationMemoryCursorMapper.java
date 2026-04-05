package edu.zsc.ai.domain.mapper.ai;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import edu.zsc.ai.domain.model.entity.ai.AiConversationMemoryCursor;

@Mapper
public interface AiConversationMemoryCursorMapper extends BaseMapper<AiConversationMemoryCursor> {
}
