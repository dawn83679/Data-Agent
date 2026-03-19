package edu.zsc.ai.domain.model.dto.request.permission;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PermissionToggleRequest {

    @NotNull
    private Boolean enabled;
}
