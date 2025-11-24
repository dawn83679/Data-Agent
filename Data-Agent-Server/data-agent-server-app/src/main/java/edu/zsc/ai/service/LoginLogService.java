package edu.zsc.ai.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;

import edu.zsc.ai.model.entity.LoginLog;

/**
 * Login Log Service
 *
 * @author Data-Agent Team
 */
public interface LoginLogService extends IService<LoginLog> {

    /**
     * Record successful login
     */
    void recordSuccess(Long userId, String email, String ipAddress, String userAgent, String loginMethod);

    /**
     * Record failed login
     */
    void recordFailure(String email, String ipAddress, String userAgent, String loginMethod, String failureReason);

    /**
     * Get user login history
     */
    List<LoginLog> getUserLoginHistory(Long userId, int limit);

    /**
     * Get recent failed login attempts by email
     */
    List<LoginLog> getRecentFailedAttempts(String email, int hours);
}
