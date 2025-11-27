package edu.zsc.ai.schedule;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import edu.zsc.ai.service.RefreshTokenService;
import edu.zsc.ai.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled Task: Clean up expired Sessions and RefreshTokens
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
     * Clean up expired Sessions and RefreshTokens daily at 2 AM
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
     * Clean up every hour (optional, for more frequent cleanup)
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
