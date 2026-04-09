package edu.zsc.ai.domain.model.entity.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import edu.zsc.ai.common.enums.org.WorkspaceTypeEnum;
import edu.zsc.ai.common.enums.permission.CatalogMatchMode;
import edu.zsc.ai.common.enums.permission.PermissionScopeType;
import edu.zsc.ai.common.enums.permission.SchemaMatchMode;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_permission_rule")
public class AiPermissionRule {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("scope_type")
    private PermissionScopeType scopeType;

    @TableField("user_id")
    private Long userId;

    @TableField("workspace_type")
    private WorkspaceTypeEnum workspaceType;

    @TableField("org_id")
    private Long orgId;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("connection_id")
    private Long connectionId;

    @TableField("catalog_name")
    private String catalogName;

    @TableField("catalog_match_mode")
    private CatalogMatchMode catalogMatchMode;

    @TableField("schema_name")
    private String schemaName;

    @TableField("schema_match_mode")
    private SchemaMatchMode schemaMatchMode;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
