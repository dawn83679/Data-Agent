package edu.zsc.ai.domain.service.ai.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.domain.mapper.AiCompressionRecordMapper;
import edu.zsc.ai.domain.model.entity.ai.AiCompressionRecord;
import edu.zsc.ai.domain.service.ai.AiCompressionRecordService;
import org.springframework.stereotype.Service;

/**
 * Service implementation for ai_compression_record operations
 *
 * @author zgq
 */
@Service
public class AiCompressionRecordServiceImpl extends ServiceImpl<AiCompressionRecordMapper, AiCompressionRecord>
        implements AiCompressionRecordService {
}