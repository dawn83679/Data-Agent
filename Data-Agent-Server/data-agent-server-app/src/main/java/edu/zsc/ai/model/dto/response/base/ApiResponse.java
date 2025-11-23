package edu.zsc.ai.model.dto.response.base;

import edu.zsc.ai.enums.error.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * Common response class
 *
 * @param <T> data type
 * @author Data-Agent Team
 */
@Data
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Status code
     */
    private int code;

    /**
     * Data
     */
    private T data;

    /**
     * Message
     */
    private String message;

    public ApiResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public ApiResponse(int code, T data) {
        this(code, data, "");
    }

    public ApiResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }

    /**
     * Success
     *
     * @param data data
     * @param <T> data type
     * @return success response
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, data, "ok");
    }

    /**
     * Success (no data)
     *
     * @param <T> data type
     * @return success response
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(0, null, "ok");
    }

    /**
     * Error
     *
     * @param errorCode error code enum
     * @param <T> data type
     * @return error response
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode);
    }

    /**
     * Error
     *
     * @param code error code
     * @param message error message
     * @param <T> data type
     * @return error response
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, null, message);
    }

    /**
     * Error (custom message)
     *
     * @param errorCode error code enum
     * @param message custom error message
     * @param <T> data type
     * @return error response
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), null, message);
    }

    /**
     * Error (message only, using default system error code)
     *
     * @param message error message
     * @param <T> data type
     * @return error response
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(ErrorCode.SYSTEM_ERROR.getCode(), null, message);
    }
}

