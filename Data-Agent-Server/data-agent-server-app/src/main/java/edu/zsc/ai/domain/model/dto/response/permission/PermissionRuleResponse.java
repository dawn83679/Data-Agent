package edu.zsc.ai.domain.model.dto.response.permission;

import edu.zsc.ai.common.enums.permission.PermissionGrantPreset;
import edu.zsc.ai.common.enums.permission.PermissionScopeType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PermissionRuleResponse {

    private Long id;
    private PermissionScopeType scopeType;
    private Long conversationId;
    private Long connectionId;
    private String connectionName;
    private PermissionGrantPreset grantPreset;
    private String catalogName;
    private String schemaName;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
