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

    /** Workspace / organization context (headers X-Workspace-Type, X-Org-Id) */
    public static final String WORKSPACE_TYPE_INVALID = "error.workspace.type.invalid";
    public static final String WORKSPACE_ORG_ID_REQUIRED = "error.workspace.org.id.required";
    public static final String WORKSPACE_ORG_INVALID = "error.workspace.org.invalid";
    public static final String WORKSPACE_ORG_NOT_MEMBER = "error.workspace.org.not.member";
    public static final String WORKSPACE_ORG_NO_ROLE = "error.workspace.org.no.role";
    public static final String WORKSPACE_ORG_ROLE_INVALID = "error.workspace.org.role.invalid";
    public static final String WORKSPACE_ORG_ID_INVALID = "error.workspace.org.id.invalid";

    public static final String WORKSPACE_COMMON_WRITE_FORBIDDEN = "error.workspace.common.write.forbidden";
    /** Organization COMMON: REST workbench (SQL, metadata, permissions, exports) and connection management */
    public static final String WORKSPACE_COMMON_WORKBENCH_FORBIDDEN = "error.workspace.common.workbench.forbidden";

    /** Organization management */
    public static final String ORG_CODE_EXISTS = "error.org.code.exists";
    public static final String ORG_ADMIN_REQUIRED = "error.org.admin.required";
    public static final String ORG_MEMBER_NOT_FOUND = "error.org.member.not.found";
    public static final String ORG_LAST_ADMIN_FORBIDDEN = "error.org.last.admin.forbidden";
    public static final String ORG_REMOVE_ADMIN_FORBIDDEN = "error.org.remove.admin.forbidden";
}
