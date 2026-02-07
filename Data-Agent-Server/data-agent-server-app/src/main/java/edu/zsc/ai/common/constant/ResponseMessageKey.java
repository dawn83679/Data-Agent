package edu.zsc.ai.common.constant;

public class ResponseMessageKey {

    private ResponseMessageKey() {
        // utility class
    }

    public static final String SUCCESS_MESSAGE = "common.success";
    public static final String SYSTEM_ERROR_MESSAGE = "error.system";
    public static final String UNAUTHORIZED_MESSAGE = "error.unauthorized";
    public static final String FORBIDDEN_MESSAGE = "error.forbidden";

    public static final String NOT_LOGIN_MESSAGE = "error.not.login";
    public static final String INVALID_CREDENTIALS_MESSAGE = "error.auth.invalid.credentials";
    public static final String INVALID_REFRESH_TOKEN_MESSAGE = "error.auth.invalid.refresh.token";
    public static final String EMAIL_OR_USERNAME_EXISTS_MESSAGE = "error.auth.email.or.username.exists";
    public static final String USER_NOT_FOUND_MESSAGE = "error.auth.user.not.found";

    public static final String USERNAME_ALREADY_EXISTS_MESSAGE = "error.user.username.already.exists";
    public static final String SESSION_NOT_FOUND_MESSAGE = "error.session.not.found";
    public static final String SESSION_NOT_BELONG_TO_USER_MESSAGE = "error.session.not.belong.to.user";

    public static final String CONNECTION_ACCESS_DENIED_MESSAGE = "error.connection.access.denied";
    public static final String CONNECTION_NOT_FOUND_MESSAGE = "error.db.connection.not.found";
    public static final String CONNECTION_NAME_EXISTS_MESSAGE = "error.db.connection.name.exists";
}
