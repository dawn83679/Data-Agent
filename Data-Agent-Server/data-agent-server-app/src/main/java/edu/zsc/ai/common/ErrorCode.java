package edu.zsc.ai.common;

import lombok.Getter;

/**
 * 自定义错误码
 *
 * @author Data-Agent Team
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败"),

    // ==================== 数据库连接相关 (50100-50199) ====================

    /**
     * 数据库连接失败
     */
    DB_CONNECTION_ERROR(50100, "数据库连接失败"),

    /**
     * 数据库连接超时
     */
    DB_CONNECTION_TIMEOUT(50101, "数据库连接超时"),

    /**
     * 数据库连接不存在
     */
    DB_CONNECTION_NOT_FOUND(50102, "数据库连接不存在"),

    /**
     * 数据库连接已存在
     */
    DB_CONNECTION_ALREADY_EXISTS(50103, "数据库连接已存在"),

    /**
     * 数据库连接配置错误
     */
    DB_CONNECTION_CONFIG_ERROR(50104, "数据库连接配置错误"),

    // ==================== 驱动相关 (50200-50299) ====================

    /**
     * 驱动文件不存在
     */
    DRIVER_NOT_FOUND(50200, "驱动文件不存在"),

    /**
     * 驱动加载失败
     */
    DRIVER_LOAD_ERROR(50201, "驱动加载失败"),

    /**
     * 驱动下载失败
     */
    DRIVER_DOWNLOAD_ERROR(50202, "驱动下载失败"),

    /**
     * 驱动版本不支持
     */
    DRIVER_VERSION_NOT_SUPPORTED(50203, "驱动版本不支持"),

    /**
     * 驱动文件损坏
     */
    DRIVER_FILE_CORRUPTED(50204, "驱动文件损坏"),

    // ==================== SQL 执行相关 (50300-50399) ====================

    /**
     * SQL 语法错误
     */
    SQL_SYNTAX_ERROR(50300, "SQL 语法错误"),

    /**
     * SQL 执行失败
     */
    SQL_EXECUTION_ERROR(50301, "SQL 执行失败"),

    /**
     * SQL 执行超时
     */
    SQL_TIMEOUT_ERROR(50302, "SQL 执行超时"),

    /**
     * 事务提交失败
     */
    TRANSACTION_COMMIT_ERROR(50303, "事务提交失败"),

    /**
     * 事务回滚失败
     */
    TRANSACTION_ROLLBACK_ERROR(50304, "事务回滚失败"),

    // ==================== 插件相关 (50400-50499) ====================

    /**
     * 插件不存在
     */
    PLUGIN_NOT_FOUND(50400, "插件不存在"),

    /**
     * 插件加载失败
     */
    PLUGIN_LOAD_ERROR(50401, "插件加载失败"),

    /**
     * 插件不支持该功能
     */
    PLUGIN_NOT_SUPPORT(50402, "插件不支持该功能"),

    /**
     * 插件初始化失败
     */
    PLUGIN_INIT_ERROR(50403, "插件初始化失败"),

    // ==================== 文件操作相关 (50500-50599) ====================

    /**
     * 文件不存在
     */
    FILE_NOT_FOUND(50500, "文件不存在"),

    /**
     * 文件读取失败
     */
    FILE_READ_ERROR(50501, "文件读取失败"),

    /**
     * 文件写入失败
     */
    FILE_WRITE_ERROR(50502, "文件写入失败"),

    /**
     * 文件删除失败
     */
    FILE_DELETE_ERROR(50503, "文件删除失败"),

    /**
     * 文件格式不支持
     */
    FILE_FORMAT_NOT_SUPPORTED(50504, "文件格式不支持"),

    /**
     * 文件大小超出限制
     */
    FILE_SIZE_EXCEEDED(50505, "文件大小超出限制"),

    // ==================== 数据验证相关 (50600-50699) ====================

    /**
     * 数据验证失败
     */
    VALIDATION_ERROR(50600, "数据验证失败"),

    /**
     * 必填字段为空
     */
    REQUIRED_FIELD_EMPTY(50601, "必填字段为空"),

    /**
     * 字段格式错误
     */
    FIELD_FORMAT_ERROR(50602, "字段格式错误"),

    /**
     * 字段长度超出限制
     */
    FIELD_LENGTH_EXCEEDED(50603, "字段长度超出限制"),

    /**
     * 字段值超出范围
     */
    FIELD_VALUE_OUT_OF_RANGE(50604, "字段值超出范围");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
