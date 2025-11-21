package edu.zsc.ai.schedule;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import edu.zsc.ai.service.RefreshTokenService;
import edu.zsc.ai.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 定时任务：清理过期的 Session 和 RefreshToken
 * 
 * @author Data-Agent Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final SessionService sessionService;
    private final RefreshTokenService refreshTokenService;

    /**
     * 每天凌晨 2 点清理过期的 Session 和 RefreshToken
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTokens() {
        log.info("Starting scheduled cleanup of expired sessions and refresh tokens");
        
        try {
            // Clean expired sessions
            sessionService.cleanExpiredSessions();
            
            // Clean expired refresh tokens
            refreshTokenService.cleanExpiredTokens();
            
            log.info("Completed scheduled cleanup successfully");
        } catch (Exception e) {
            log.error("Error during scheduled cleanup", e);
        }
    }
    
    /**
     * 每小时清理一次（可选，用于更频繁的清理）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void hourlyCleanup() {
        log.debug("Running hourly cleanup check");
        
        try {
            sessionService.cleanExpiredSessions();
            refreshTokenService.cleanExpiredTokens();
        } catch (Exception e) {
            log.error("Error during hourly cleanup", e);
        }
    }
}
