package edu.zsc.ai.domain.model.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity for sys_sessions table
 *
 * @author zgq
 */
@Data
@TableName("sys_sessions")
public class SysSessions {

    /**
     * Session unique identifier, primary key
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * User ID, references sys_users table
     */
    @TableField("user_id")
    private Long userId;

    /**
     * Access token hash value, not stored in plain text
     */
    @TableField("access_token_hash")
    private String accessTokenHash;

    /**
     * Login IP address
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * User agent string for device identification
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * Session active status: 0=revoked, 1=active
     */
    @TableField("active")
    private Integer active;

    /**
     * Last refresh time, for cleaning up long-unused sessions
     */
    @TableField("last_refresh_at")
    private LocalDateTime lastRefreshAt;

    /**
     * Session creation time
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    /**
     * Session information last update time
     */
    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;
}