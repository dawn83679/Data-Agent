package edu.zsc.ai.exception.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import edu.zsc.ai.common.ErrorCode;
import edu.zsc.ai.exception.BusinessException;
import edu.zsc.ai.model.dto.response.ApiResponse;
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

