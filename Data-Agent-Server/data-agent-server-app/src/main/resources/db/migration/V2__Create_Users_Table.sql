-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    password VARCHAR(255),  -- Nullable: supports email-code-only and OAuth users
    username VARCHAR(100) NOT NULL,
    avatar VARCHAR(500),
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    oauth_provider VARCHAR(50),
    oauth_provider_id VARCHAR(255),
    status INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delete_flag INTEGER DEFAULT 0
);

-- Create indexes with performance optimization
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email) WHERE delete_flag = 0;
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone) WHERE delete_flag = 0;
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status) WHERE delete_flag = 0;
CREATE INDEX IF NOT EXISTS idx_users_oauth ON users(oauth_provider, oauth_provider_id);

-- Add comments
COMMENT ON TABLE users IS 'User table';
COMMENT ON COLUMN users.email IS 'User email (unique)';
COMMENT ON COLUMN users.phone IS 'Phone number';
COMMENT ON COLUMN users.password IS 'Password (BCrypt encrypted, nullable for email-code-only users)';
COMMENT ON COLUMN users.username IS 'Username';
COMMENT ON COLUMN users.avatar IS 'Avatar URL';
COMMENT ON COLUMN users.email_verified IS 'Email verified flag';
COMMENT ON COLUMN users.phone_verified IS 'Phone verified flag';
COMMENT ON COLUMN users.oauth_provider IS 'OAuth provider (google, github, etc.)';
COMMENT ON COLUMN users.oauth_provider_id IS 'OAuth provider user ID';
COMMENT ON COLUMN users.status IS 'Account status (0: normal, 1: disabled)';
COMMENT ON COLUMN users.delete_flag IS 'Logical delete flag (0: not deleted, 1: deleted)';
COMMENT ON INDEX idx_users_email IS 'Optimize user lookup by email during login';
COMMENT ON INDEX idx_users_phone IS 'Optimize user lookup by phone during login';
