package edu.zsc.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.zsc.ai.model.entity.EmailVerificationCode;
import org.apache.ibatis.annotations.Mapper;

/**
 * Email Verification Code Mapper
 *
 * @author Data-Agent Team
 */
@Mapper
public interface EmailVerificationCodeMapper extends BaseMapper<EmailVerificationCode> {
}
