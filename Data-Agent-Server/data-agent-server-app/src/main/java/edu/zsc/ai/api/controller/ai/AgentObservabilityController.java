package edu.zsc.ai.api.controller.ai;

import edu.zsc.ai.api.model.request.ai.AgentObservabilityUpdateRequest;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.service.ai.AgentObservabilityAdminService;
import edu.zsc.ai.observability.config.AgentObservabilitySnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/observability")
@RequiredArgsConstructor
public class AgentObservabilityController {

    private final AgentObservabilityAdminService adminService;

    @GetMapping
    public ApiResponse<AgentObservabilitySnapshot> getSnapshot() {
        return ApiResponse.success(adminService.getSnapshot());
    }

    @PutMapping
    public ApiResponse<AgentObservabilitySnapshot> updateRuntimeOverride(@RequestBody AgentObservabilityUpdateRequest request) {
        return ApiResponse.success(adminService.updateRuntimeOverride(request));
    }

    @DeleteMapping("/runtime-override")
    public ApiResponse<AgentObservabilitySnapshot> clearRuntimeOverride() {
        return ApiResponse.success(adminService.clearRuntimeOverride());
    }
}
