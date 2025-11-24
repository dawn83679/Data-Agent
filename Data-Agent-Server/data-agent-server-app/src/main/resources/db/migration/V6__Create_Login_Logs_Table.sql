-- Create login_logs table
CREATE TABLE IF NOT EXISTS login_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    email VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    login_method VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_login_logs_user_id ON login_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_login_logs_email ON login_logs(email);
CREATE INDEX IF NOT EXISTS idx_login_logs_ip_address ON login_logs(ip_address);
CREATE INDEX IF NOT EXISTS idx_login_logs_status ON login_logs(status);
CREATE INDEX IF NOT EXISTS idx_login_logs_create_time ON login_logs(create_time DESC);
CREATE INDEX IF NOT EXISTS idx_login_logs_user_time ON login_logs(user_id, create_time DESC);

-- Add comments
COMMENT ON TABLE login_logs IS 'Login audit log table';
COMMENT ON COLUMN login_logs.user_id IS 'User ID (nullable for failed logins)';
COMMENT ON COLUMN login_logs.email IS 'Login email address';
COMMENT ON COLUMN login_logs.ip_address IS 'Client IP address';
COMMENT ON COLUMN login_logs.user_agent IS 'Client user agent';
COMMENT ON COLUMN login_logs.login_method IS 'Login method (PASSWORD, EMAIL_CODE, GOOGLE_OAUTH)';
COMMENT ON COLUMN login_logs.status IS 'Login status (SUCCESS, FAILED)';
COMMENT ON COLUMN login_logs.failure_reason IS 'Failure reason if status is FAILED';
COMMENT ON INDEX idx_login_logs_user_time IS 'Optimize user login history queries';
