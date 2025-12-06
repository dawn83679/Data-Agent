package edu.zsc.ai.enums;

import lombok.Getter;

/**
 * Session Status Enum
 * 
 * @author Data-Agent Team
 */
@Getter
public enum SessionStatus {
    
    /**
     * Session is active and valid
     */
    ACTIVE(0, "Active"),
    
    /**
     * Session has expired
     */
    EXPIRED(1, "Expired"),
    
    /**
     * Session has been revoked by user or system
     */
    REVOKED(2, "Revoked");

    private final Integer code;
    private final String description;

    SessionStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Get enum by code
     * 
     * @param code status code
     * @return SessionStatus enum
     */
    public static SessionStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (SessionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * Check if session is active
     * 
     * @param code status code
     * @return true if active
     */
    public static boolean isActive(Integer code) {
        return ACTIVE.code.equals(code);
    }

    /**
     * Check if session is expired
     * 
     * @param code status code
     * @return true if expired
     */
    public static boolean isExpired(Integer code) {
        return EXPIRED.code.equals(code);
    }

    /**
     * Check if session is revoked
     * 
     * @param code status code
     * @return true if revoked
     */
    public static boolean isRevoked(Integer code) {
        return REVOKED.code.equals(code);
    }
}
