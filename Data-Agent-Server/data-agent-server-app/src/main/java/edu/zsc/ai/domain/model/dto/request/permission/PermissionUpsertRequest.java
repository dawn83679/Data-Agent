package edu.zsc.ai.domain.model.dto.request.permission;

import edu.zsc.ai.common.enums.permission.PermissionGrantPreset;
import edu.zsc.ai.common.enums.permission.PermissionScopeType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PermissionUpsertRequest {

    @NotNull
    private PermissionScopeType scopeType;

    private Long conversationId;

    @NotNull
    private Long connectionId;

    @NotNull
    private PermissionGrantPreset grantPreset;

    private String catalogName;

    private String schemaName;

    @NotNull
    private Boolean enabled;
}
