package edu.zsc.ai.model.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * Email Verification Code Entity
 *
 * @author Data-Agent Team
 */
@Data
@TableName("sys_email_verification_codes")
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
