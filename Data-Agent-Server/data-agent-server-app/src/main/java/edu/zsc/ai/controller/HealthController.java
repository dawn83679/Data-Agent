package edu.zsc.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import edu.zsc.ai.model.dto.response.ApiResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Basic health check controller that exposes a liveness endpoint.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    /**
     * Liveness probe endpoint.
     *
     * @return service status details
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "DataAgent");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("version", "0.0.1-SNAPSHOT");
        
        return ApiResponse.success(healthInfo);
    }
}

