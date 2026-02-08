package edu.zsc.ai.common.enums.error;

import lombok.Getter;

/**
 * Custom error codes (3-digit format for simplicity)
 */
@Getter
public enum ErrorCodeEnum {

    // ==================== Common (200) ====================
    SUCCESS(200, "common.success"),

    // ==================== Client Errors (400-499) ====================
    PARAMS_ERROR(400, "error.params"),
    NOT_LOGIN_ERROR(401, "error.not.login"),
    NO_AUTH_ERROR(403, "error.no.auth"),
    NOT_FOUND_ERROR(404, "error.not.found"),

    // ==================== Server Errors (500-599) ====================
    SYSTEM_ERROR(500, "error.system"),
    OPERATION_ERROR(501, "error.operation"),

    // ==================== Database Connection (510-519) ====================
    DB_CONNECTION_ERROR(510, "error.db.connection"),
    DB_CONNECTION_TIMEOUT(511, "error.db.connection.timeout"),
    DB_CONNECTION_NOT_FOUND(512, "error.db.connection.not.found"),
    DB_CONNECTION_ALREADY_EXISTS(513, "error.db.connection.already.exists"),
    DB_CONNECTION_CONFIG_ERROR(514, "error.db.connection.config"),

    // ==================== Driver (520-529) ====================
    DRIVER_NOT_FOUND(520, "error.driver.not.found"),
    DRIVER_LOAD_ERROR(521, "error.driver.load"),
    DRIVER_DOWNLOAD_ERROR(522, "error.driver.download"),
    DRIVER_VERSION_NOT_SUPPORTED(523, "error.driver.version.not.supported"),
    DRIVER_FILE_CORRUPTED(524, "error.driver.file.corrupted"),

    // ==================== SQL Execution (530-539) ====================
    SQL_SYNTAX_ERROR(530, "error.sql.syntax"),
    SQL_EXECUTION_ERROR(531, "error.sql.execution"),
    SQL_TIMEOUT_ERROR(532, "error.sql.timeout"),
    TRANSACTION_COMMIT_ERROR(533, "error.transaction.commit"),
    TRANSACTION_ROLLBACK_ERROR(534, "error.transaction.rollback"),

    // ==================== Plugin (540-549) ====================
    PLUGIN_NOT_FOUND(540, "error.plugin.not.found"),
    PLUGIN_LOAD_ERROR(541, "error.plugin.load"),
    PLUGIN_NOT_SUPPORT(542, "error.plugin.not.support"),
    PLUGIN_INIT_ERROR(543, "error.plugin.init"),

    // ==================== File Operation (550-559) ====================
    FILE_NOT_FOUND(550, "error.file.not.found"),
    FILE_READ_ERROR(551, "error.file.read"),
    FILE_WRITE_ERROR(552, "error.file.write"),
    FILE_DELETE_ERROR(553, "error.file.delete"),
    FILE_FORMAT_NOT_SUPPORTED(554, "error.file.format.not.supported"),
    FILE_SIZE_EXCEEDED(555, "error.file.size.exceeded"),

    // ==================== Data Validation (560-569) ====================
    VALIDATION_ERROR(560, "error.validation"),
    REQUIRED_FIELD_EMPTY(561, "error.required.field.empty"),
    FIELD_FORMAT_ERROR(562, "error.field.format"),
    FIELD_LENGTH_EXCEEDED(563, "error.field.length.exceeded"),
    FIELD_VALUE_OUT_OF_RANGE(564, "error.field.value.out.of.range");

    private final int code;
    private final String messageKey;

    ErrorCodeEnum(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }

    public String getMessage() {
        return messageKey;
    }
}
