package edu.zsc.ai.domain.model.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity for sys_organization_member_roles.
 */
@Data
@TableName("sys_organization_member_roles")
public class SysOrganizationMemberRole {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("organization_member_id")
    private Long organizationMemberId;

    @TableField("role_code")
    private String roleCode;

    @TableField("active")
    private Boolean active;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
