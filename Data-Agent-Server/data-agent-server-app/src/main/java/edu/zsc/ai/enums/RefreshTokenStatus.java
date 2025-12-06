package edu.zsc.ai.enums;

import lombok.Getter;

/**
 * Refresh Token Status Enum
 * 
 * @author Data-Agent Team
 */
@Getter
public enum RefreshTokenStatus {
    
    /**
     * Token is active and can be used
     */
    ACTIVE(0, "Active"),
    
    /**
     * Token has been revoked and cannot be used
     */
    REVOKED(1, "Revoked");

    private final Integer code;
    private final String description;

    RefreshTokenStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Get enum by code
     * 
     * @param code status code
     * @return RefreshTokenStatus enum
     */
    public static RefreshTokenStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (RefreshTokenStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * Check if token is active
     * 
     * @param code status code
     * @return true if active
     */
    public static boolean isActive(Integer code) {
        return ACTIVE.code.equals(code);
    }

    /**
     * Check if token is revoked
     * 
     * @param code status code
     * @return true if revoked
     */
    public static boolean isRevoked(Integer code) {
        return REVOKED.code.equals(code);
    }
}
