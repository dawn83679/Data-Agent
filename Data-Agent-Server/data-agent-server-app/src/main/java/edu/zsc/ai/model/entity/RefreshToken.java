package edu.zsc.ai.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Refresh Token Entity
 * Stores refresh tokens for token renewal
 *
 * @author Data-Agent Team
 */
@Data
@TableName("refresh_tokens")
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
     * Token status (0: active, 1: used, 2: revoked)
     */
    private Integer status;

    /**
     * Used at (when token was used for refresh)
     */
    private LocalDateTime usedAt;

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
}
