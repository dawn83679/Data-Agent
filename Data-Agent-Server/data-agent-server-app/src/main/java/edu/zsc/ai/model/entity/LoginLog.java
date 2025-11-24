package edu.zsc.ai.model.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * Login Log Entity
 *
 * @author Data-Agent Team
 */
@Data
@TableName("login_logs")
public class LoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String email;

    private String ipAddress;

    private String userAgent;

    private String loginMethod;

    private String status;

    private String failureReason;

    private LocalDateTime createTime;
}
