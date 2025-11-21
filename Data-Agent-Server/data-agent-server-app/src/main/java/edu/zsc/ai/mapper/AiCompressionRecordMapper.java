package edu.zsc.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.zsc.ai.model.entity.ai.AiCompressionRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper interface for ai_compression_record table
 *
 * @author zgq
 */
@Mapper
public interface AiCompressionRecordMapper extends BaseMapper<AiCompressionRecord> {
}