package edu.zsc.ai.domain.model.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity for sys_organization_connection_permissions.
 */
@Data
@TableName("sys_organization_connection_permissions")
public class SysOrganizationConnectionPermission {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("org_id")
    private Long orgId;

    @TableField("connection_id")
    private Long connectionId;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("granted_by")
    private Long grantedBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
