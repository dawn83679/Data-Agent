package edu.zsc.ai.enums;

import lombok.Getter;

/**
 * Session Status Enum
 * 
 * @author Data-Agent Team
 */
@Getter
public enum SessionStatus {
    ACTIVE(0, "Active"),
    EXPIRED(1, "Expired"),
    REVOKED(2, "Revoked");

    private final Integer code;
    private final String description;

    SessionStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
