package edu.zsc.ai.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Login Event
 * Published when a login attempt occurs
 *
 * @author Data-Agent Team
 */
@Getter
public class LoginEvent extends ApplicationEvent {

    private final String email;
    private final String ipAddress;
    private final String userAgent;
    private final boolean success;
    private final String failureReason;

    public LoginEvent(Object source, String email, String ipAddress, 
                     String userAgent, boolean success, String failureReason) {
        super(source);
        this.email = email;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.success = success;
        this.failureReason = failureReason;
    }

    public static LoginEvent success(Object source, String email, String ipAddress, String userAgent) {
        return new LoginEvent(source, email, ipAddress, userAgent, true, null);
    }

    public static LoginEvent failure(Object source, String email, String ipAddress, 
                                    String userAgent, String reason) {
        return new LoginEvent(source, email, ipAddress, userAgent, false, reason);
    }
}
