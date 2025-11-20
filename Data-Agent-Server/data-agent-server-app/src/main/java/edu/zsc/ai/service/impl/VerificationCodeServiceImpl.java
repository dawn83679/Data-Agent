package edu.zsc.ai.service.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import edu.zsc.ai.enums.error.ErrorCode;
import edu.zsc.ai.exception.BusinessException;
import edu.zsc.ai.mapper.EmailVerificationCodeMapper;
import edu.zsc.ai.model.entity.EmailVerificationCode;
import edu.zsc.ai.service.EmailService;
import edu.zsc.ai.service.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Verification Code Service Implementation
 *
 * @author Data-Agent Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeServiceImpl extends ServiceImpl<EmailVerificationCodeMapper, EmailVerificationCode>
        implements VerificationCodeService {

    private final EmailService emailService;
    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRY_MINUTES = 5;
    private static final int SEND_INTERVAL_SECONDS = 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional
    public boolean sendCode(String email, String codeType, String ipAddress) {
        // 1. Check if code was sent recently (rate limiting)
        LocalDateTime recentTime = LocalDateTime.now().minusSeconds(SEND_INTERVAL_SECONDS);
        long recentCount = this.count(new LambdaQueryWrapper<EmailVerificationCode>()
                .eq(EmailVerificationCode::getEmail, email)
                .eq(EmailVerificationCode::getCodeType, codeType)
                .ge(EmailVerificationCode::getCreateTime, recentTime));

        if (recentCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "Verification code sent recently. Please wait " + SEND_INTERVAL_SECONDS + " seconds");
        }

        // 2. Generate verification code
        String code = generateCode();

        // 3. Save to database
        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode(code);
        verificationCode.setCodeType(codeType);
        verificationCode.setIpAddress(ipAddress);
        verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES));
        verificationCode.setVerified(false);

        boolean saved = this.save(verificationCode);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to save verification code");
        }

        // 4. Send email (async recommended in production)
        try {
            emailService.sendVerificationCode(email, code, codeType);
        } catch (Exception e) {
            log.error("Failed to send verification code email: email={}, codeType={}", email, codeType, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to send verification code email");
        }

        log.info("Verification code sent: email={}, codeType={}", email, codeType);
        return true;
    }

    @Override
    @Transactional
    public boolean verifyCode(String email, String code, String codeType) {
        // 1. Find valid code
        EmailVerificationCode verificationCode = this.getOne(new LambdaQueryWrapper<EmailVerificationCode>()
                .eq(EmailVerificationCode::getEmail, email)
                .eq(EmailVerificationCode::getCode, code)
                .eq(EmailVerificationCode::getCodeType, codeType)
                .eq(EmailVerificationCode::getVerified, false)
                .gt(EmailVerificationCode::getExpiresAt, LocalDateTime.now())
                .orderByDesc(EmailVerificationCode::getCreateTime)
                .last("LIMIT 1"));

        if (verificationCode == null) {
            log.warn("Invalid or expired verification code: email={}, codeType={}", email, codeType);
            return false;
        }

        // 2. Mark as verified
        verificationCode.setVerified(true);
        verificationCode.setVerifiedAt(LocalDateTime.now());
        boolean updated = this.updateById(verificationCode);

        if (updated) {
            log.info("Verification code verified: email={}, codeType={}", email, codeType);
        }

        return updated;
    }

    @Override
    public void cleanExpiredCodes() {
        // Delete codes expired more than 1 day ago
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);
        int deleted = this.baseMapper.delete(new LambdaQueryWrapper<EmailVerificationCode>()
                .lt(EmailVerificationCode::getExpiresAt, threshold));

        if (deleted > 0) {
            log.info("Cleaned {} expired verification codes", deleted);
        }
    }

    /**
     * Generate random verification code
     */
    private String generateCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(RANDOM.nextInt(10));
        }
        return code.toString();
    }
}
