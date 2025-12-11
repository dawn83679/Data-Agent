package edu.zsc.ai.exception.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cn.dev33.satoken.exception.NotLoginException;
import edu.zsc.ai.enums.error.ErrorCode;
import edu.zsc.ai.exception.BusinessException;
import edu.zsc.ai.model.dto.response.base.ApiResponse;
import edu.zsc.ai.util.I18nUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler
 * Support i18n messages
 *
 * @author hhz
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private I18nUtils i18nUtils;

    /**
     * Handle Sa-Token NotLoginException
     *
     * @param e not login exception
     * @return unified response format
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotLoginException(NotLoginException e) {
        String message;
        switch (e.getType()) {
            case NotLoginException.NOT_TOKEN:
                message = i18nUtils.getMessage("error.token.missing");
                break;
            case NotLoginException.INVALID_TOKEN:
                message = i18nUtils.getMessage("error.token.invalid");
                break;
            case NotLoginException.TOKEN_TIMEOUT:
                message = i18nUtils.getMessage("error.token.expired");
                break;
            case NotLoginException.BE_REPLACED:
                message = i18nUtils.getMessage("error.token.replaced");
                break;
            case NotLoginException.KICK_OUT:
                message = i18nUtils.getMessage("error.token.kickout");
                break;
            default:
                message = i18nUtils.getMessage("error.not.login");
        }
        
        log.warn("Not login exception: type={}, message={}", e.getType(), message);
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.NOT_LOGIN_ERROR, message));
    }

    /**
     * Handle business exception
     *
     * @param e business exception
     * @return unified response format
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.error("Business exception: {}", e.getMessage());
        String message = i18nUtils.getMessage(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.error(e.getCode(), message));
    }

    /**
     * Handle validation exception
     *
     * @param e validation exception
     * @return unified response format
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : i18nUtils.getMessage("error.validation");
        log.error("Validation exception: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.PARAMS_ERROR, message));
    }

    /**
     * Handle bind exception
     *
     * @param e bind exception
     * @return unified response format
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : i18nUtils.getMessage("error.params");
        log.error("Bind exception: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.PARAMS_ERROR, message));
    }

    /**
     * Handle all other uncaught exceptions
     *
     * @param e exception
     * @return unified response format
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("System exception: ", e);
        String systemError = i18nUtils.getMessage("error.system");
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.SYSTEM_ERROR, systemError + ": " + e.getMessage()));
    }
}

