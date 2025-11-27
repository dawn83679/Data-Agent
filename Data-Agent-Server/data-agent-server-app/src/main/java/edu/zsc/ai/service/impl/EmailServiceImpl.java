package edu.zsc.ai.service.impl;

import edu.zsc.ai.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Email Service Implementation
 * TODO: Integrate with actual email service provider (e.g., SendGrid, AWS SES, Aliyun)
 *
 * @author Data-Agent Team
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Override
    public void sendVerificationCode(String to, String code, String codeType) {
        // TODO: Implement actual email sending logic
        // For now, just log the code (for development/testing)
        log.info("Sending verification code email: to={}, codeType={}, code={}", to, codeType, code);
        
        // Example implementation with email service:
        // String subject = getSubjectByCodeType(codeType);
        // String content = buildEmailContent(code, codeType);
        // emailClient.send(to, subject, content);
        
        // For development, you can print to console
        System.out.println("=".repeat(50));
        System.out.println("Verification Code Email");
        System.out.println("To: " + to);
        System.out.println("Type: " + codeType);
        System.out.println("Code: " + code);
        System.out.println("=".repeat(50));
    }

    private String getSubjectByCodeType(String codeType) {
        return switch (codeType) {
            case "LOGIN" -> "Login Verification Code";
            case "REGISTER" -> "Registration Verification Code";
            case "RESET_PASSWORD" -> "Reset Password Verification Code";
            default -> "Verification Code";
        };
    }
}
