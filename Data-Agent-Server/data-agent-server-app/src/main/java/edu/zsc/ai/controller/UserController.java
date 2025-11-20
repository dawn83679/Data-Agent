package edu.zsc.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.zsc.ai.model.dto.response.UserInfoResponse;
import edu.zsc.ai.model.dto.response.base.ApiResponse;
import edu.zsc.ai.service.AuthService;
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

    // TODO: Future endpoints for user management
    // @PutMapping("/me") - Update user profile
    // @PutMapping("/me/password") - Change password
    // @GetMapping("/me/sessions") - List active sessions
    // @DeleteMapping("/me/sessions/{sessionId}") - Revoke specific session
    // @DeleteMapping("/me/sessions") - Revoke all sessions except current
}
