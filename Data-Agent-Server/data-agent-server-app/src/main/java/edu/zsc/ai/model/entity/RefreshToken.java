package edu.zsc.ai.model.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * Refresh Token Entity
 * Stores refresh tokens for token renewal
 *
 * @author Data-Agent Team
 */
@Data
@TableName("sys_refresh_tokens")
public class RefreshToken {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Session ID (foreign key to sessions table)
     */
    private Long sessionId;

    /**
     * Refresh Token Hash (SHA-256)
     */
    private String tokenHash;

    /**
     * Token expiration time
     */
    private LocalDateTime expiresAt;

    /**
     * Token revoked status (0: not revoked, 1: revoked)
     */
    private Integer revoked;

    /**
     * Last used at (when token was last used)
     */
    private LocalDateTime lastUsedAt;

    /**
     * Created at
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * Updated at
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
