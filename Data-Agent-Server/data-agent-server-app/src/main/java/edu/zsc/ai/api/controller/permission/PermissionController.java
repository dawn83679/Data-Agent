package edu.zsc.ai.api.controller.permission;

import edu.zsc.ai.common.enums.permission.PermissionScopeType;
import edu.zsc.ai.domain.model.dto.request.permission.PermissionApproveRequest;
import edu.zsc.ai.domain.model.dto.request.permission.PermissionToggleRequest;
import edu.zsc.ai.domain.model.dto.request.permission.PermissionUpsertRequest;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.model.dto.response.permission.PermissionRuleResponse;
import edu.zsc.ai.domain.service.permission.PermissionRuleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionRuleService permissionRuleService;

    @GetMapping("/rules")
    public ApiResponse<List<PermissionRuleResponse>> listRules(@RequestParam(required = false) PermissionScopeType scopeType,
                                                               @RequestParam(required = false) Long conversationId) {
        return ApiResponse.success(permissionRuleService.listForCurrentUser(scopeType, conversationId));
    }

    @PostMapping("/rules")
    public ApiResponse<PermissionRuleResponse> upsertRule(@Valid @RequestBody PermissionUpsertRequest request) {
        return ApiResponse.success(permissionRuleService.upsertForCurrentUser(
                request.getScopeType(),
                request.getConversationId(),
                request.getConnectionId(),
                request.getGrantPreset(),
                request.getCatalogName(),
                request.getSchemaName(),
                Boolean.TRUE.equals(request.getEnabled())
        ));
    }

    @PatchMapping("/rules/{id}/enabled")
    public ApiResponse<Void> setRuleEnabled(@PathVariable @NotNull Long id,
                                            @Valid @RequestBody PermissionToggleRequest request) {
        permissionRuleService.toggleForCurrentUser(id, Boolean.TRUE.equals(request.getEnabled()));
        return ApiResponse.success();
    }

    @DeleteMapping("/rules/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable @NotNull Long id) {
        permissionRuleService.deleteForCurrentUser(id);
        return ApiResponse.success();
    }

    @PostMapping("/write-executions/approve")
    public ApiResponse<Void> approveWriteExecution(@Valid @RequestBody PermissionApproveRequest request) {
        permissionRuleService.approveWriteExecution(
                request.getConversationId(),
                request.getConnectionId(),
                request.getCatalogName(),
                request.getSchemaName(),
                request.getSql(),
                request.getScopeType(),
                request.getGrantPreset()
        );
        return ApiResponse.success();
    }
}
