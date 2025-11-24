package edu.zsc.ai.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * User Entity
 *
 * @author Data-Agent Team
 */
@Data
@TableName("users")
public class User {

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", emailVerified=" + emailVerified +
                ", status=" + status +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Email (unique, used for login)
     */
    private String email;

    /**
     * Phone number
     */
    private String phone;

    /**
     * Password (BCrypt encrypted)
     */
    private String password;

    /**
     * Username
     */
    private String username;

    /**
     * Avatar URL
     */
    private String avatar;

    /**
     * Email verified flag
     */
    private Boolean emailVerified;

    /**
     * Phone verified flag
     */
    private Boolean phoneVerified;

    /**
     * OAuth provider (null for email/password, 'google' for Google OAuth)
     */
    private String oauthProvider;

    /**
     * OAuth provider user ID
     */
    private String oauthProviderId;

    /**
     * Account status (0: normal, 1: disabled)
     */
    private Integer status;

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

    /**
     * Logical delete flag
     */
    @TableLogic
    private Integer deleteFlag;
}
