package edu.zsc.ai.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Login Event Listener
 * Handles login events asynchronously for audit logging
 *
 * @author Data-Agent Team
 */
@Slf4j
@Component
public class LoginEventListener {

    @Async
    @EventListener
    public void handleLoginEvent(LoginEvent event) {
        if (event.isSuccess()) {
            log.info("Login SUCCESS - Email: {}, IP: {}, UserAgent: {}", 
                    event.getEmail(), 
                    event.getIpAddress(), 
                    event.getUserAgent());
        } else {
            log.warn("Login FAILED - Email: {}, IP: {}, Reason: {}, UserAgent: {}", 
                    event.getEmail(), 
                    event.getIpAddress(), 
                    event.getFailureReason(),
                    event.getUserAgent());
        }
        
        // TODO: In production, save to audit log table or send to monitoring system
    }
}
