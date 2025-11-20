package edu.zsc.ai.exception;

import edu.zsc.ai.enums.error.ErrorCode;
import lombok.Getter;

/**
 * 自定义业务异常类
 *
 * @author Data-Agent Team
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    /**
     * 构造函数：使用错误码枚举
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 构造函数：使用错误码枚举和自定义消息
     *
     * @param errorCode 错误码枚举
     * @param message 自定义错误消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    /**
     * 构造函数：使用错误码枚举、自定义消息和异常原因
     *
     * @param errorCode 错误码枚举
     * @param message 自定义错误消息
     * @param cause 异常原因
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
    }

    /**
     * 构造函数：直接指定错误码和消息（兼容旧代码）
     *
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}

