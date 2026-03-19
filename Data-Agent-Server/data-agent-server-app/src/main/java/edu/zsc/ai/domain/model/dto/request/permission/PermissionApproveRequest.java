package edu.zsc.ai.domain.model.dto.request.permission;

import edu.zsc.ai.common.enums.permission.PermissionGrantPreset;
import edu.zsc.ai.common.enums.permission.PermissionScopeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PermissionApproveRequest {

    @NotNull
    private Long conversationId;

    @NotNull
    private Long connectionId;

    private String catalogName;

    private String schemaName;

    @NotBlank
    private String sql;

    private PermissionScopeType scopeType;

    private PermissionGrantPreset grantPreset;
}
