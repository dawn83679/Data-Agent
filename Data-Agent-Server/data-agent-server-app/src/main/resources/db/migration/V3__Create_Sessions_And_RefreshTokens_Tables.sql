-- Create sessions table
CREATE TABLE IF NOT EXISTS sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    access_token_hash VARCHAR(512) NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    device_info VARCHAR(255),
    last_activity_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_refresh_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    status INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create refresh_tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    token_hash VARCHAR(512) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    status INTEGER DEFAULT 0,
    used_at TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE
);

-- Create indexes with performance optimization
CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_access_token_hash ON sessions(access_token_hash) WHERE status = 0;
CREATE INDEX IF NOT EXISTS idx_sessions_status ON sessions(status);
CREATE INDEX IF NOT EXISTS idx_sessions_status_expires ON sessions(status, expires_at);
CREATE INDEX IF NOT EXISTS idx_sessions_expires_at ON sessions(expires_at);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_session_id ON refresh_tokens(session_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash ON refresh_tokens(token_hash) WHERE status = 0;
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_status ON refresh_tokens(status);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_status_expires ON refresh_tokens(status, expires_at);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- Add comments
COMMENT ON TABLE sessions IS 'User session table';
COMMENT ON COLUMN sessions.access_token_hash IS 'SHA-256 hash of access token for security (64 characters hex string)';
COMMENT ON COLUMN sessions.status IS 'Session status (0: active, 1: expired, 2: revoked)';
COMMENT ON INDEX idx_sessions_access_token_hash IS 'Optimize session validation by token hash';

COMMENT ON TABLE refresh_tokens IS 'Refresh token table';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'Refresh token hash or value - supports up to 512 characters';
COMMENT ON COLUMN refresh_tokens.status IS 'Token status (0: active, 1: used, 2: revoked)';
COMMENT ON INDEX idx_refresh_tokens_token_hash IS 'Optimize refresh token validation';
