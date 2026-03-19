package edu.zsc.ai.agent.tool.sql.model;

import edu.zsc.ai.common.enums.permission.PermissionGrantPreset;
import edu.zsc.ai.common.enums.permission.PermissionScopeType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WriteExecutionGrantOption {

    private PermissionScopeType scopeType;
    private PermissionGrantPreset grantPreset;
}
