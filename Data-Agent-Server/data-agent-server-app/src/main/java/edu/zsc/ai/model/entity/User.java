package edu.zsc.ai.model.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * User Entity
 *
 * @author Data-Agent Team
 */
@Data
@TableName("sys_users")
public class User {

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", verified=" + verified +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Username, can be duplicated
     */
    private String username;

    /**
     * Email address for login, globally unique
     */
    private String email;

    /**
     * Password hash value, encrypted by application layer
     */
    private String passwordHash;

    /**
     * Phone number
     */
    private String phone;

    /**
     * Avatar image URL address
     */
    private String avatarUrl;

    /**
     * Email verification status: false=not verified, true=verified
     */
    private Boolean verified;

    /**
     * OAuth provider (null for email/password, 'google' for Google OAuth)
     */
    private String oauthProvider;

    /**
     * OAuth provider user ID
     */
    private String oauthProviderId;

    /**
     * Account creation time
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * Account information last update time
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
