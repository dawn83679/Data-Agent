package edu.zsc.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import edu.zsc.ai.model.dto.response.ApiResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 * Provides basic health check endpoint
 */
@Tag(name = "System Health", description = "System health check APIs")
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Operation(summary = "Health Check", description = "Check service health status")
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

