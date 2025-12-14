-- Add auth_provider column to sys_users table
ALTER TABLE sys_users ADD COLUMN auth_provider VARCHAR(20) DEFAULT 'EMAIL';
COMMENT ON COLUMN sys_users.auth_provider IS 'Authentication provider: EMAIL, GOOGLE, GITHUB';

-- Update existing records to have 'EMAIL' as default provider
UPDATE sys_users SET auth_provider = 'EMAIL' WHERE auth_provider IS NULL;
