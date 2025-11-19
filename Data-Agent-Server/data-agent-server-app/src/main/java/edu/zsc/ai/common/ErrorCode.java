package edu.zsc.ai.common;

import lombok.Getter;

/**
 * 自定义错误码
 * 使用国际化 key 作为消息
 *
 * @author hhz
 */
@Getter
public enum ErrorCode {

    // ==================== Common Response (0-99) ====================
    
    /**
     * Operation successful
     */
    SUCCESS(0, "common.success"),
    
    // ==================== Client Errors (40000-49999) ====================
    
    /**
     * Request parameters error
     */
    PARAMS_ERROR(40000, "error.params"),
    
    /**
     * User not logged in
     */
    NOT_LOGIN_ERROR(40100, "error.not.login"),
    
    /**
     * No permission
     */
    NO_AUTH_ERROR(40101, "error.no.auth"),
    
    /**
     * Resource not found
     */
    NOT_FOUND_ERROR(40400, "error.not.found"),
    
    /**
     * Access forbidden
     */
    FORBIDDEN_ERROR(40300, "error.forbidden"),
    
    /**
     * Account locked due to too many failed login attempts
     */
    ACCOUNT_LOCKED_ERROR(42900, "error.account.locked"),

    /**
     * Verification code error
     */
    VERIFICATION_CODE_ERROR(40001, "error.verification.code"),
    
    // ==================== Server Errors (50000-50099) ====================
    
    /**
     * System internal error
     */
    SYSTEM_ERROR(50000, "error.system"),
    
    /**
     * Operation failed
     */
    OPERATION_ERROR(50001, "error.operation"),

    // ==================== Database Connection (50100-50199) ====================

    /**
     * Database connection failed
     */
    DB_CONNECTION_ERROR(50100, "error.db.connection"),

    /**
     * Database connection timeout
     */
    DB_CONNECTION_TIMEOUT(50101, "error.db.connection.timeout"),

    /**
     * Database connection not found
     */
    DB_CONNECTION_NOT_FOUND(50102, "error.db.connection.not.found"),

    /**
     * Database connection already exists
     */
    DB_CONNECTION_ALREADY_EXISTS(50103, "error.db.connection.already.exists"),

    /**
     * Database connection configuration error
     */
    DB_CONNECTION_CONFIG_ERROR(50104, "error.db.connection.config"),

    // ==================== Driver (50200-50299) ====================

    /**
     * Driver file not found
     */
    DRIVER_NOT_FOUND(50200, "error.driver.not.found"),

    /**
     * Driver loading failed
     */
    DRIVER_LOAD_ERROR(50201, "error.driver.load"),

    /**
     * Driver download failed
     */
    DRIVER_DOWNLOAD_ERROR(50202, "error.driver.download"),

    /**
     * Driver version not supported
     */
    DRIVER_VERSION_NOT_SUPPORTED(50203, "error.driver.version.not.supported"),

    /**
     * Driver file corrupted
     */
    DRIVER_FILE_CORRUPTED(50204, "error.driver.file.corrupted"),

    // ==================== SQL Execution (50300-50399) ====================

    /**
     * SQL syntax error
     */
    SQL_SYNTAX_ERROR(50300, "error.sql.syntax"),

    /**
     * SQL execution failed
     */
    SQL_EXECUTION_ERROR(50301, "error.sql.execution"),

    /**
     * SQL execution timeout
     */
    SQL_TIMEOUT_ERROR(50302, "error.sql.timeout"),

    /**
     * Transaction commit failed
     */
    TRANSACTION_COMMIT_ERROR(50303, "error.transaction.commit"),

    /**
     * Transaction rollback failed
     */
    TRANSACTION_ROLLBACK_ERROR(50304, "error.transaction.rollback"),

    // ==================== Plugin (50400-50499) ====================

    /**
     * Plugin not found
     */
    PLUGIN_NOT_FOUND(50400, "error.plugin.not.found"),

    /**
     * Plugin loading failed
     */
    PLUGIN_LOAD_ERROR(50401, "error.plugin.load"),

    /**
     * Plugin does not support this feature
     */
    PLUGIN_NOT_SUPPORT(50402, "error.plugin.not.support"),

    /**
     * Plugin initialization failed
     */
    PLUGIN_INIT_ERROR(50403, "error.plugin.init"),

    // ==================== File Operation (50500-50599) ====================

    /**
     * File not found
     */
    FILE_NOT_FOUND(50500, "error.file.not.found"),

    /**
     * File reading failed
     */
    FILE_READ_ERROR(50501, "error.file.read"),

    /**
     * File writing failed
     */
    FILE_WRITE_ERROR(50502, "error.file.write"),

    /**
     * File deletion failed
     */
    FILE_DELETE_ERROR(50503, "error.file.delete"),

    /**
     * File format not supported
     */
    FILE_FORMAT_NOT_SUPPORTED(50504, "error.file.format.not.supported"),

    /**
     * File size exceeded limit
     */
    FILE_SIZE_EXCEEDED(50505, "error.file.size.exceeded"),

    // ==================== Data Validation (50600-50699) ====================

    /**
     * Data validation failed
     */
    VALIDATION_ERROR(50600, "error.validation"),

    /**
     * Required field is empty
     */
    REQUIRED_FIELD_EMPTY(50601, "error.required.field.empty"),

    /**
     * Field format error
     */
    FIELD_FORMAT_ERROR(50602, "error.field.format"),

    /**
     * Field length exceeded limit
     */
    FIELD_LENGTH_EXCEEDED(50603, "error.field.length.exceeded"),

    /**
     * Field value out of range
     */
    FIELD_VALUE_OUT_OF_RANGE(50604, "error.field.value.out.of.range");

    /**
     * Status code
     */
    private final int code;

    /**
     * I18n message key
     */
    private final String messageKey;

    ErrorCode(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }

    /**
     * Get i18n message key
     * Compatible with old code using getMessage()
     */
    public String getMessage() {
        return messageKey;
    }
}
