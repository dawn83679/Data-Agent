package edu.zsc.ai.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Session Entity
 * Tracks active user sessions
 *
 * @author Data-Agent Team
 */
@Data
@TableName("sessions")
public class Session {

    @Override
    public String toString() {
        return "Session{" +
                "id=" + id +
                ", userId=" + userId +
                ", ipAddress='" + ipAddress + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", lastActivityAt=" + lastActivityAt +
                ", expiresAt=" + expiresAt +
                ", status=" + status +
                ", createTime=" + createTime +
                '}';
    }

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Access Token Hash (SHA-256)
     */
    private String accessTokenHash;

    /**
     * Client IP address
     */
    private String ipAddress;

    /**
     * User Agent
     */
    private String userAgent;

    /**
     * Device info (optional)
     */
    private String deviceInfo;

    /**
     * Last activity time
     */
    private LocalDateTime lastActivityAt;

    /**
     * Last refresh time
     */
    private LocalDateTime lastRefreshAt;

    /**
     * Session expiration time
     */
    private LocalDateTime expiresAt;

    /**
     * Session status (0: active, 1: expired, 2: revoked)
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
}
