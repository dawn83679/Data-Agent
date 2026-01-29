package edu.zsc.ai.common.constant;

/**
 * OAuth related constants
 */
public class OAuthConstant {

    // Oauth provider
    public static final String GOOGLE_PROVIDER = "google";
    public static final String GITHUB_PROVIDER = "github";

    // Google OAuth
    public static final String GOOGLE_AUTHORIZATION_URI = "https://accounts.google.com/o/oauth2/v2/auth";
    public static final String GOOGLE_TOKEN_URI = "https://oauth2.googleapis.com/token";
    public static final String GOOGLE_SCOPE = "openid email profile";
    public static final String GOOGLE_ACCESS_TYPE = "online";
    public static final String GOOGLE_RESPONSE_TYPE = "code";
    public static final String GOOGLE_GRANT_TYPE = "authorization_code";
    public static final String GOOGLE_PROMPT = "consent";

    // GitHub OAuth
    public static final String GITHUB_AUTHORIZATION_URI = "https://github.com/login/oauth/authorize";
    public static final String GITHUB_TOKEN_URI = "https://github.com/login/oauth/access_token";
    public static final String GITHUB_USER_INFO_URI = "https://api.github.com/user";
    public static final String GITHUB_USER_EMAILS_URI = "https://api.github.com/user/emails";
    public static final String GITHUB_SCOPE = "user:email";

    // Common Parameters
    public static final String PARAM_CLIENT_ID = "client_id";
    public static final String PARAM_CLIENT_SECRET = "client_secret";
    public static final String PARAM_REDIRECT_URI = "redirect_uri";
    public static final String PARAM_RESPONSE_TYPE = "response_type";
    public static final String PARAM_SCOPE = "scope";
    public static final String PARAM_STATE = "state";
    public static final String PARAM_ACCESS_TYPE = "access_type";
    public static final String PARAM_PROMPT = "prompt";
    public static final String PARAM_NONCE = "nonce";
    public static final String PARAM_CODE = "code";
    public static final String PARAM_GRANT_TYPE = "grant_type";
    public static final String PARAM_PROMPT_VALUE_SELECT_ACCOUNT = "select_account";

    // Response Keys
    public static final String KEY_ID_TOKEN = "id_token";
    public static final String KEY_ACCESS_TOKEN = "access_token";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_NAME = "name";
    public static final String KEY_PICTURE = "picture";
    public static final String KEY_SUB = "sub";
    public static final String KEY_ID = "id";
    public static final String KEY_LOGIN = "login";
    public static final String KEY_AVATAR_URL = "avatar_url";
    public static final String KEY_PRIMARY = "primary";
    public static final String KEY_VERIFIED = "verified";
}
