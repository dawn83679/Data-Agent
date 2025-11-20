package edu.zsc.ai.model.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity for sys_users table
 *
 * @author zgq
 */
@Data
@TableName("sys_users")
public class SysUsers {

    /**
     * User unique identifier, primary key
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Username, can be duplicated
     */
    @TableField("username")
    private String username;

    /**
     * Email address for login, globally unique
     */
    @TableField("email")
    private String email;

    /**
     * Password hash value, encrypted by application layer
     */
    @TableField("password_hash")
    private String passwordHash;

    /**
     * Phone number
     */
    @TableField("phone")
    private String phone;

    /**
     * Avatar image URL address
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * Email verification status: false=not verified, true=verified
     */
    @TableField("verified")
    private Boolean verified;

    /**
     * Account creation time
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    /**
     * Account information last update time
     */
    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;
}