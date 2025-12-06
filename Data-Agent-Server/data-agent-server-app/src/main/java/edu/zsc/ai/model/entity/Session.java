package edu.zsc.ai.model.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * System Session Entity
 * Tracks active user sessions
 *
 * @author Data-Agent Team
 */
@Data
@TableName("sys_sessions")
public class Session {

    @Override
    public String toString() {
        return "Session{" +
                "id=" + id +
                ", userId=" + userId +
                ", ipAddress='" + ipAddress + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", lastRefreshAt=" + lastRefreshAt +
                ", expiresAt=" + expiresAt +
                ", status=" + status +
                ", createdAt=" + createdAt +
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
    private LocalDateTime createdAt;

    /**
     * Update time
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
