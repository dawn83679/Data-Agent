package org.tech.ai.dataagent.exception;

import lombok.Getter;

/**
 * Business Exception
 * Used for business logic errors
 */
@Getter
public class BusinessException extends RuntimeException {
    
    /**
     * Error code
     */
    private final Integer code;
    
    /**
     * Constructor with code and message
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
    
    /**
     * Constructor with message only (default code 500)
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }
    
    /**
     * Constructor with code, message and cause
     */
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}

