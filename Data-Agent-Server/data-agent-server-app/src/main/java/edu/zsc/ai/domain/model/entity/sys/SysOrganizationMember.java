package edu.zsc.ai.domain.model.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity for sys_organization_members.
 */
@Data
@TableName("sys_organization_members")
public class SysOrganizationMember {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("org_id")
    private Long orgId;

    @TableField("user_id")
    private Long userId;

    /**
     * 1 = active, 0 = inactive
     */
    @TableField("status")
    private Integer status;

    @TableField("joined_at")
    private LocalDateTime joinedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
