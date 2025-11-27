-- Add OAuth columns to sys_users table
ALTER TABLE sys_users 
ADD COLUMN IF NOT EXISTS oauth_provider VARCHAR(50),
ADD COLUMN IF NOT EXISTS oauth_provider_id VARCHAR(255);

-- Create index for OAuth fields
CREATE INDEX IF NOT EXISTS idx_sys_users_oauth ON sys_users(oauth_provider, oauth_provider_id);

-- Add comments
COMMENT ON COLUMN sys_users.oauth_provider IS 'OAuth provider (google, github, etc.)';
COMMENT ON COLUMN sys_users.oauth_provider_id IS 'OAuth provider user ID';
