package edu.zsc.ai.util.exception;

import edu.zsc.ai.common.constant.ResponseConstant;
import lombok.Getter;

/**
 * Custom business exception class
 *
 * @author Data-Agent Team
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = ResponseConstant.SYSTEM_ERROR;
    }

    // ========== Basic Factory Methods ==========

    public static BusinessException of(int code, String message) {
        return new BusinessException(code, message);
    }

    public static BusinessException of(String message) {
        return new BusinessException(ResponseConstant.SYSTEM_ERROR, message);
    }

    public static BusinessException of() {
        return new BusinessException(ResponseConstant.SYSTEM_ERROR, ResponseConstant.SYSTEM_ERROR_MESSAGE);
    }

    // ========== Conditional Throw Methods ==========

    /**
     * Throw exception if condition is true
     */
    public static void throwIf(boolean condition, int code, String message) {
        if (condition) {
            throw new BusinessException(code, message);
        }
    }

    /**
     * Throw exception if condition is true (default 500 error code)
     */
    public static void throwIf(boolean condition, String message) {
        if (condition) {
            throw new BusinessException(ResponseConstant.SYSTEM_ERROR, message);
        }
    }

    // ========== Assertion Methods ==========

    /**
     * Assert object is not null, otherwise throw exception
     */
    public static <T> T assertNotNull(T object, int code, String message) {
        if (object == null) {
            throw new BusinessException(code, message);
        }
        return object;
    }

    /**
     * Assert object is not null, otherwise throw 404 exception
     */
    public static <T> T assertNotNull(T object, String message) {
        return assertNotNull(object, ResponseConstant.NOT_FOUND, message);
    }

    /**
     * Assert condition is true, otherwise throw exception
     */
    public static void assertTrue(boolean condition, int code, String message) {
        if (!condition) {
            throw new BusinessException(code, message);
        }
    }

    /**
     * Assert condition is true, otherwise throw exception (default 400 error code)
     */
    public static void assertTrue(boolean condition, String message) {
        assertTrue(condition, ResponseConstant.PARAM_ERROR, message);
    }

    /**
     * Assert condition is false, otherwise throw exception
     */
    public static void assertFalse(boolean condition, int code, String message) {
        if (condition) {
            throw new BusinessException(code, message);
        }
    }

    /**
     * Assert condition is false, otherwise throw exception (default 400 error code)
     */
    public static void assertFalse(boolean condition, String message) {
        assertFalse(condition, ResponseConstant.PARAM_ERROR, message);
    }

    // ========== Common HTTP Status Code Convenience Methods ==========

    /**
     * Throw 400 error (parameter error)
     */
    public static BusinessException badRequest(String message) {
        return new BusinessException(ResponseConstant.PARAM_ERROR, message);
    }

    /**
     * Throw 400 error (formatted message)
     */
    public static BusinessException badRequest(String format, Object... args) {
        return new BusinessException(ResponseConstant.PARAM_ERROR, String.format(format, args));
    }

    /**
     * Throw 401 error (unauthorized)
     */
    public static BusinessException unauthorized(String message) {
        return new BusinessException(ResponseConstant.UNAUTHORIZED, message);
    }

    /**
     * Throw 401 error (default message)
     */
    public static BusinessException unauthorized() {
        return new BusinessException(ResponseConstant.UNAUTHORIZED, ResponseConstant.UNAUTHORIZED_MESSAGE);
    }

    /**
     * Throw 403 error (forbidden)
     */
    public static BusinessException forbidden(String message) {
        return new BusinessException(ResponseConstant.FORBIDDEN, message);
    }

    /**
     * Throw 403 error (default message)
     */
    public static BusinessException forbidden() {
        return new BusinessException(ResponseConstant.FORBIDDEN, ResponseConstant.FORBIDDEN_MESSAGE);
    }

    /**
     * Throw 404 error (resource not found)
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(ResponseConstant.NOT_FOUND, message);
    }

    /**
     * Throw 404 error (formatted message)
     */
    public static BusinessException notFound(String format, Object... args) {
        return new BusinessException(ResponseConstant.NOT_FOUND, String.format(format, args));
    }

    /**
     * Throw 500 error (system error)
     */
    public static BusinessException serverError(String message) {
        return new BusinessException(ResponseConstant.SYSTEM_ERROR, message);
    }

    /**
     * Throw 500 error (default message)
     */
    public static BusinessException serverError() {
        return new BusinessException(ResponseConstant.SYSTEM_ERROR, ResponseConstant.SYSTEM_ERROR_MESSAGE);
    }
}

