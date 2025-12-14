package edu.zsc.ai.common.constant;

public class ResponseConstant {

    public static final int SUCCESS = 200;
    public static final int SYSTEM_ERROR = 500;
    public static final int PARAM_ERROR = 400;
    public static final int NOT_FOUND = 404;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int TOO_MANY_REQUESTS = 429;
    public static final int SERVICE_UNAVAILABLE = 503;
    public static final int GATEWAY_TIMEOUT = 504;
    public static final String SUCCESS_MESSAGE = "success";
    public static final String SYSTEM_ERROR_MESSAGE = "system error";
    public static final String PARAM_ERROR_MESSAGE = "param error";
    public static final String NOT_FOUND_MESSAGE = "not found";
    public static final String UNAUTHORIZED_MESSAGE = "unauthorized";
    public static final String FORBIDDEN_MESSAGE = "forbidden";
    public static final String TOO_MANY_REQUESTS_MESSAGE = "too many requests";
    public static final String SERVICE_UNAVAILABLE_MESSAGE = "service unavailable";
    public static final String GATEWAY_TIMEOUT_MESSAGE = "gateway timeout";

    // auth error messages
    public static final String INVALID_CREDENTIALS_MESSAGE = "invalid email or password";
    public static final String INVALID_REFRESH_TOKEN_MESSAGE = "invalid or expired refresh token";
    public static final String EMAIL_OR_USERNAME_EXISTS_MESSAGE = "email or username already exists";
    public static final String USER_NOT_FOUND_MESSAGE = "user not found";
    public static final String EMAIL_NOT_VERIFIED_MESSAGE = "email not verified";

    // unified error messages
    public static final String NOT_LOGIN_MESSAGE = "not login";
    public static final String FORBIDDEN_MESSAGE_DETAIL = "permission denied";
    public static final String PARAM_INVALID_MESSAGE = "parameter invalid";

    // user error messages
    public static final String USERNAME_ALREADY_EXISTS_MESSAGE = "用户名已被占用";
    public static final String SESSION_NOT_FOUND_MESSAGE = "会话不存在";
    public static final String CANNOT_DELETE_CURRENT_SESSION_MESSAGE = "不能删除当前会话";
    public static final String SESSION_NOT_BELONG_TO_USER_MESSAGE = "该会话不属于当前用户";

    // google oauth error messages
    public static final String GOOGLE_OAUTH_AUTHORIZATION_FAILED = "GOOGLE_OAUTH_AUTHORIZATION_FAILED";
    public static final String GOOGLE_OAUTH_MISSING_CODE = "GOOGLE_OAUTH_MISSING_CODE";
    public static final String GOOGLE_OAUTH_TOKEN_EXCHANGE_FAILED = "GOOGLE_OAUTH_TOKEN_EXCHANGE_FAILED";
    public static final String GOOGLE_OAUTH_MISSING_ID_TOKEN = "GOOGLE_OAUTH_MISSING_ID_TOKEN";
    public static final String GOOGLE_OAUTH_MISSING_EMAIL = "GOOGLE_OAUTH_MISSING_EMAIL";
    public static final String GOOGLE_OAUTH_CALLBACK_PROCESSING_FAILED = "GOOGLE_OAUTH_CALLBACK_PROCESSING_FAILED";
    public static final String GOOGLE_OAUTH_SERVICE_UNAVAILABLE = "GOOGLE_OAUTH_SERVICE_UNAVAILABLE";

}