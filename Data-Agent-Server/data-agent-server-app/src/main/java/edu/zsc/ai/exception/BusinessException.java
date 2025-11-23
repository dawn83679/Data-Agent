package edu.zsc.ai.exception;

import edu.zsc.ai.enums.error.ErrorCode;
import lombok.Getter;

/**
 * Custom business exception class
 *
 * @author Data-Agent Team
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * Error code
     */
    private final int code;

    /**
     * Constructor: using error code enum
     *
     * @param errorCode error code enum
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * Constructor: using error code enum and custom message
     *
     * @param errorCode error code enum
     * @param message custom error message
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    /**
     * Constructor: using error code enum, custom message and exception cause
     *
     * @param errorCode error code enum
     * @param message custom error message
     * @param cause exception cause
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
    }

    /**
     * Constructor: directly specify error code and message (compatible with legacy code)
     *
     * @param code error code
     * @param message error message
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}

