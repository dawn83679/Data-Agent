package edu.zsc.ai.enums;

import lombok.Getter;

/**
 * Refresh Token Status Enum
 * 
 * @author Data-Agent Team
 */
@Getter
public enum RefreshTokenStatus {
    ACTIVE(0, "Active"),
    REVOKED(1, "Revoked");

    private final Integer code;
    private final String description;

    RefreshTokenStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
