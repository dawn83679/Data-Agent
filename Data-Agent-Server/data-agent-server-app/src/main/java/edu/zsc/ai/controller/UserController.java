package edu.zsc.ai.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.model.dto.response.SessionResponse;
import edu.zsc.ai.model.dto.response.UserInfoResponse;
import edu.zsc.ai.model.dto.response.base.ApiResponse;
import edu.zsc.ai.model.entity.Session;
import edu.zsc.ai.service.AuthService;
import edu.zsc.ai.service.SessionService;
import edu.zsc.ai.util.HashUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * User Controller
 * Handles user resource management: profile, settings, sessions
 * All endpoints require authentication
 *
 * @author Data-Agent Team
 */
@Tag(name = "User", description = "User resource management APIs - Profile, Settings, Sessions")
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final SessionService sessionService;

    /**
     * Get current user profile
     * Requires authentication
     */
    @Operation(summary = "Get Current User", description = "Get current logged-in user profile information")
    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> getCurrentUser(
            @RequestHeader("Authorization") String authorization) {
        // Extract token from "Bearer {token}"
        String token = authorization.replace("Bearer ", "");
        log.debug("Get current user profile");
        UserInfoResponse userInfo = authService.getCurrentUser(token);
        return ApiResponse.success(userInfo);
    }

    /**
     * Get all active sessions for current user
     * Requires authentication
     */
    @Operation(summary = "Get Active Sessions", description = "Get all active sessions for current user")
    @GetMapping("/sessions")
    public ApiResponse<List<SessionResponse>> getActiveSessions(
            @RequestHeader("Authorization") String authorization) {
        // Get current user ID
        Long userId = StpUtil.getLoginIdAsLong();
        
        // Get current session ID to mark it
        String token = authorization.replace("Bearer ", "");
        String accessTokenHash = HashUtil.sha256(token);
        Session currentSession = sessionService.getByAccessToken(accessTokenHash);
        Long currentSessionId = currentSession != null ? currentSession.getId() : null;
        
        // Get all active sessions
        List<Session> sessions = sessionService.getUserActiveSessions(userId);
        
        // Convert to response DTOs
        List<SessionResponse> sessionResponses = sessions.stream()
                .map(session -> SessionResponse.builder()
                        .id(session.getId())
                        .ipAddress(session.getIpAddress())
                        .userAgent(session.getUserAgent())
                        .deviceInfo(session.getDeviceInfo())
                        .lastActivityAt(session.getLastActivityAt())
                        .createdAt(session.getCreatedAt())
                        .isCurrent(session.getId().equals(currentSessionId))
                        .build())
                .collect(Collectors.toList());
        
        log.info("User {} retrieved {} active sessions", userId, sessionResponses.size());
        return ApiResponse.success(sessionResponses);
    }

    /**
     * Revoke a specific session (kick out device)
     * Requires authentication
     */
    @Operation(summary = "Revoke Session", description = "Revoke a specific session (kick out device)")
    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Boolean> revokeSession(
            @PathVariable Long sessionId) {
        // Get current user ID
        Long userId = StpUtil.getLoginIdAsLong();
        
        // Revoke session with permission check
        sessionService.revokeSessionWithPermissionCheck(userId, sessionId);
        
        log.info("User {} revoked session: sessionId={}", userId, sessionId);
        return ApiResponse.success(true);
    }

    /**
     * Revoke all other sessions (keep only current device)
     * Requires authentication
     */
    @Operation(summary = "Revoke Other Sessions", description = "Revoke all other sessions (keep only current device)")
    @DeleteMapping("/sessions/others")
    public ApiResponse<Boolean> revokeOtherSessions(
            @RequestHeader("Authorization") String authorization) {
        // Get current user ID
        Long userId = StpUtil.getLoginIdAsLong();
        
        // Get current session ID
        String token = authorization.replace("Bearer ", "");
        String accessTokenHash = HashUtil.sha256(token);
        Session currentSession = sessionService.getByAccessToken(accessTokenHash);
        
        if (currentSession == null) {
            log.warn("Current session not found for user: {}", userId);
            return ApiResponse.success(false);
        }
        
        // Revoke all other sessions
        sessionService.revokeOtherSessions(userId, currentSession.getId());
        
        log.info("User {} revoked all other sessions, keeping sessionId={}", userId, currentSession.getId());
        return ApiResponse.success(true);
    }

    /**
     * Update current user profile
     * Requires authentication
     */
    @Operation(summary = "Update User Profile", description = "Update current user's profile information")
    @org.springframework.web.bind.annotation.PutMapping("/me")
    public ApiResponse<Boolean> updateProfile(
            @org.springframework.web.bind.annotation.RequestBody @jakarta.validation.Valid 
            edu.zsc.ai.model.dto.request.UpdateUserProfileRequest request) {
        // Get current user ID
        Long userId = StpUtil.getLoginIdAsLong();
        
        // Update profile
        boolean result = authService.updateUserProfile(userId, request.getUsername(), 
            request.getAvatar(), request.getPhone());
        
        log.info("User {} updated profile", userId);
        return ApiResponse.success(result);
    }
}
