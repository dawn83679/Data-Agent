package edu.zsc.ai.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.zsc.ai.model.dto.response.base.ApiResponse;
import edu.zsc.ai.service.CacheMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin Controller
 * Handles system administration tasks: cache management, monitoring
 * 
 * @author Data-Agent Team
 */
@Tag(name = "Admin", description = "System administration APIs - Cache management and monitoring")
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CacheMonitorService cacheMonitorService;

    /**
     * Get cache statistics
     * Returns counts of cached sessions and refresh tokens
     */
    @Operation(summary = "Get Cache Stats", description = "Get Redis cache statistics including session and refresh token counts")
    @GetMapping("/cache/stats")
    public ApiResponse<Map<String, Object>> getCacheStats() {
        log.debug("Getting cache statistics");
        Map<String, Object> stats = cacheMonitorService.getCacheStats();
        return ApiResponse.success(stats);
    }

    /**
     * Clear all session cache
     * Use with caution - will force users to re-authenticate
     */
    @Operation(summary = "Clear Session Cache", description = "Clear all session cache entries from Redis")
    @DeleteMapping("/cache/sessions")
    public ApiResponse<String> clearSessionCache() {
        log.warn("Clearing all session cache");
        cacheMonitorService.clearSessionCache();
        return ApiResponse.success("Session cache cleared successfully");
    }

    /**
     * Clear all refresh token cache
     * Use with caution - will invalidate all refresh tokens
     */
    @Operation(summary = "Clear Refresh Token Cache", description = "Clear all refresh token cache entries from Redis")
    @DeleteMapping("/cache/refresh-tokens")
    public ApiResponse<String> clearRefreshTokenCache() {
        log.warn("Clearing all refresh token cache");
        cacheMonitorService.clearRefreshTokenCache();
        return ApiResponse.success("Refresh token cache cleared successfully");
    }

    /**
     * Clear all cache
     * Use with extreme caution - will clear all cached data
     */
    @Operation(summary = "Clear All Cache", description = "Clear all cache entries from Redis (sessions and refresh tokens)")
    @DeleteMapping("/cache/all")
    public ApiResponse<String> clearAllCache() {
        log.warn("Clearing ALL cache");
        cacheMonitorService.clearAllCache();
        return ApiResponse.success("All cache cleared successfully");
    }
}
