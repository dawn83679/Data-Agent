CREATE TABLE sys_users
(
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50) NOT NULL,
    email         VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone         VARCHAR(20),
    avatar_url    VARCHAR(500),
    verified      BOOLEAN   DEFAULT false,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_users IS 'User information table, stores basic information of system users';
COMMENT ON COLUMN sys_users.id IS 'User unique identifier, primary key';
COMMENT ON COLUMN sys_users.username IS 'Username, can be duplicated';
COMMENT ON COLUMN sys_users.email IS 'Email address for login, globally unique';
COMMENT ON COLUMN sys_users.password_hash IS 'Password hash value, encrypted by application layer';
COMMENT ON COLUMN sys_users.phone IS 'Phone number';
COMMENT ON COLUMN sys_users.avatar_url IS 'Avatar image URL address';
COMMENT ON COLUMN sys_users.verified IS 'Email verification status: false=not verified, true=verified';
COMMENT ON COLUMN sys_users.created_at IS 'Account creation time';
COMMENT ON COLUMN sys_users.updated_at IS 'Account information last update time';

-- sys_users表索引
CREATE INDEX idx_sys_users_email ON sys_users (email);

CREATE TABLE sys_refresh_tokens
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT              NOT NULL,
    token_hash   VARCHAR(255) UNIQUE NOT NULL,
    session_id   BIGINT              NOT NULL,
    expires_at   TIMESTAMP           NOT NULL,
    last_used_at TIMESTAMP,
    revoked      SMALLINT  DEFAULT 0,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_refresh_tokens IS 'Refresh token table, manages refresh tokens';
COMMENT ON COLUMN sys_refresh_tokens.id IS 'Refresh token unique identifier, primary key';
COMMENT ON COLUMN sys_refresh_tokens.user_id IS 'User ID, references sys_users table';
COMMENT ON COLUMN sys_refresh_tokens.token_hash IS 'Refresh token hash value, not stored in plain text, globally unique';
COMMENT ON COLUMN sys_refresh_tokens.session_id IS 'Associated session ID, references sys_sessions table';
COMMENT ON COLUMN sys_refresh_tokens.expires_at IS 'Refresh token expiration time';
COMMENT ON COLUMN sys_refresh_tokens.last_used_at IS 'Last used time, for activity statistics';
COMMENT ON COLUMN sys_refresh_tokens.revoked IS 'Usage status: 0=not used, 1=used';
COMMENT ON COLUMN sys_refresh_tokens.created_at IS 'Token creation time';
COMMENT ON COLUMN sys_refresh_tokens.updated_at IS 'Token information last update time';

CREATE TABLE sys_sessions
(
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    access_token_hash VARCHAR(255) NOT NULL,
    device_info       VARCHAR(500),
    ip_address        VARCHAR(45),
    user_agent        TEXT,
    active            SMALLINT  DEFAULT 1,
    last_refresh_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_sessions IS 'User session table, manages user login sessions and access tokens';
COMMENT ON COLUMN sys_sessions.id IS 'Session unique identifier, primary key';
COMMENT ON COLUMN sys_sessions.user_id IS 'User ID, references sys_users table';
COMMENT ON COLUMN sys_sessions.access_token_hash IS 'Access token hash value, not stored in plain text';
COMMENT ON COLUMN sys_sessions.device_info IS 'Device information (device type, operating system, browser, etc.)';
COMMENT ON COLUMN sys_sessions.ip_address IS 'Login IP address';
COMMENT ON COLUMN sys_sessions.user_agent IS 'User agent string for device identification';
COMMENT ON COLUMN sys_sessions.active IS 'Session active status: 0=revoked, 1=active';
COMMENT ON COLUMN sys_sessions.last_refresh_at IS 'Last refresh time, for cleaning up long-unused sessions';
COMMENT ON COLUMN sys_sessions.created_at IS 'Session creation time';
COMMENT ON COLUMN sys_sessions.updated_at IS 'Session information last update time';

-- sys_refresh_tokens表索引
CREATE INDEX idx_sys_refresh_tokens_user_id ON sys_refresh_tokens (user_id);
CREATE INDEX idx_sys_refresh_tokens_token_hash ON sys_refresh_tokens (token_hash);
CREATE INDEX idx_sys_refresh_tokens_session_id ON sys_refresh_tokens (session_id);
CREATE INDEX idx_sys_refresh_tokens_revoked ON sys_refresh_tokens (revoked);

-- sys_sessions表索引
CREATE INDEX idx_sys_sessions_user_id ON sys_sessions (user_id);
CREATE INDEX idx_sys_sessions_access_token_hash ON sys_sessions (access_token_hash);
