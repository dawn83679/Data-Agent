package edu.zsc.ai.model.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Session Response DTO
 * Contains session information for user session management
 *
 * @author Data-Agent Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {

    /**
     * Session ID
     */
    private Long id;

    /**
     * Client IP address
     */
    private String ipAddress;

    /**
     * User Agent (browser/device info)
     */
    private String userAgent;

    /**
     * Device info (optional)
     */
    private String deviceInfo;

    /**
     * Last activity time
     */
    private LocalDateTime lastActivityAt;

    /**
     * Session creation time
     */
    private LocalDateTime createdAt;

    /**
     * Whether this is the current session
     */
    private Boolean isCurrent;
}
