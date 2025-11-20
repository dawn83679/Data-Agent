package edu.zsc.ai.model.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity for sys_refresh_tokens table
 *
 * @author zgq
 */
@Data
@TableName("sys_refresh_tokens")
public class SysRefreshTokens {

    /**
     * Refresh token unique identifier, primary key
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * User ID, references sys_users table
     */
    @TableField("user_id")
    private Long userId;

    /**
     * Refresh token hash value, not stored in plain text, globally unique
     */
    @TableField("token_hash")
    private String tokenHash;

    /**
     * Associated session ID, references sys_sessions table
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * Refresh token expiration time
     */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    /**
     * Last used time, for activity statistics
     */
    @TableField("last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * Usage status: 0=not used, 1=used
     */
    @TableField("revoked")
    private Integer revoked;

    /**
     * Token creation time
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    /**
     * Token information last update time
     */
    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;
}