package edu.zsc.ai.service.impl.ai;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.mapper.AiCompressionRecordMapper;
import edu.zsc.ai.model.entity.ai.AiCompressionRecord;
import edu.zsc.ai.service.ai.AiCompressionRecordService;
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