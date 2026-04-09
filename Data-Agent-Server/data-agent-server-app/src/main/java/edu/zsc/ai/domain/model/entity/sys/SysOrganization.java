package edu.zsc.ai.domain.model.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity for sys_organizations.
 */
@Data
@TableName("sys_organizations")
public class SysOrganization {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("org_code")
    private String orgCode;

    @TableField("org_name")
    private String orgName;

    /**
     * 1 = enabled, 0 = disabled
     */
    @TableField("status")
    private Integer status;

    @TableField("remark")
    private String remark;

    @TableField("created_by")
    private Long createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
