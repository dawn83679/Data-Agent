package edu.zsc.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.model.entity.EmailVerificationCode;

/**
 * Verification Code Service Interface
 *
 * @author Data-Agent Team
 */
public interface VerificationCodeService extends IService<EmailVerificationCode> {

    /**
     * Send verification code
     *
     * @param email email address
     * @param codeType code type
     * @param ipAddress IP address
     * @return success or not
     */
    boolean sendCode(String email, String codeType, String ipAddress);

    /**
     * Verify code
     *
     * @param email email address
     * @param code verification code
     * @param codeType code type
     * @return valid or not
     */
    boolean verifyCode(String email, String code, String codeType);

    /**
     * Clean expired codes
     */
    void cleanExpiredCodes();
}
