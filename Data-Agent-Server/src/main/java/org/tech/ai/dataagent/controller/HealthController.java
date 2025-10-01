package org.tech.ai.dataagent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tech.ai.dataagent.model.dto.response.ApiResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 * Provides basic health check endpoint
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    /**
     * Health check endpoint
     * 
     * @return Service status information
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

