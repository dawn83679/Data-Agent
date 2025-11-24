package edu.zsc.ai.event;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import edu.zsc.ai.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Login Event Listener
 * Handles login events asynchronously for audit logging
 *
 * @author Data-Agent Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginEventListener {

    private final LoginLogService loginLogService;

    @Async
    @EventListener
    public void handleLoginEvent(LoginEvent event) {
        if (event.isSuccess()) {
            log.info("Login SUCCESS - Email: {}, IP: {}, UserAgent: {}", 
                    event.getEmail(), 
                    event.getIpAddress(), 
                    event.getUserAgent());
            
            // Record successful login to database
            // Note: userId will be set by AuthService when calling recordSuccess
        } else {
            log.warn("Login FAILED - Email: {}, IP: {}, Reason: {}, UserAgent: {}", 
                    event.getEmail(), 
                    event.getIpAddress(), 
                    event.getFailureReason(),
                    event.getUserAgent());
            
            // Record failed login to database
            loginLogService.recordFailure(
                event.getEmail(),
                event.getIpAddress(),
                event.getUserAgent(),
                "PASSWORD", // Default method, can be enhanced
                event.getFailureReason()
            );
        }
    }
}
