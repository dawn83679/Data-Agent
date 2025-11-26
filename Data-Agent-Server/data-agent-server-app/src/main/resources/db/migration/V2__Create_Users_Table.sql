-- Create sys_users table
CREATE TABLE IF NOT EXISTS sys_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    verified BOOLEAN DEFAULT false,
    oauth_provider VARCHAR(50),
    oauth_provider_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_sys_users_email ON sys_users(email);
CREATE INDEX IF NOT EXISTS idx_sys_users_oauth ON sys_users(oauth_provider, oauth_provider_id);

-- Add comments
COMMENT ON TABLE sys_users IS 'User information table, stores basic information of system users';
COMMENT ON COLUMN sys_users.id IS 'User unique identifier, primary key';
COMMENT ON COLUMN sys_users.username IS 'Username, can be duplicated';
COMMENT ON COLUMN sys_users.email IS 'Email address for login, globally unique';
COMMENT ON COLUMN sys_users.password_hash IS 'Password hash value, encrypted by application layer (nullable for OAuth users)';
COMMENT ON COLUMN sys_users.phone IS 'Phone number';
COMMENT ON COLUMN sys_users.avatar_url IS 'Avatar image URL address';
COMMENT ON COLUMN sys_users.verified IS 'Email verification status: false=not verified, true=verified';
COMMENT ON COLUMN sys_users.oauth_provider IS 'OAuth provider (google, github, etc.)';
COMMENT ON COLUMN sys_users.oauth_provider_id IS 'OAuth provider user ID';
COMMENT ON COLUMN sys_users.created_at IS 'Account creation time';
COMMENT ON COLUMN sys_users.updated_at IS 'Account information last update time';
