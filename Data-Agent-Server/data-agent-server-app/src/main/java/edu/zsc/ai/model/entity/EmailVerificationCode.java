package edu.zsc.ai.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Email Verification Code Entity
 *
 * @author Data-Agent Team
 */
@Data
@TableName("email_verification_codes")
public class EmailVerificationCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Email address
     */
    private String email;

    /**
     * Verification code
     */
    private String code;

    /**
     * Code type: LOGIN, REGISTER, RESET_PASSWORD
     */
    private String codeType;

    /**
     * Request IP address
     */
    private String ipAddress;

    /**
     * Expiration time
     */
    private LocalDateTime expiresAt;

    /**
     * Whether verified (0: not verified, 1: verified)
     */
    private Boolean verified;

    /**
     * Verification time
     */
    private LocalDateTime verifiedAt;

    /**
     * Create time
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * Update time
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Override
    public String toString() {
        return "EmailVerificationCode{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", codeType='" + codeType + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", expiresAt=" + expiresAt +
                ", verified=" + verified +
                ", createTime=" + createTime +
                '}';
    }
}
