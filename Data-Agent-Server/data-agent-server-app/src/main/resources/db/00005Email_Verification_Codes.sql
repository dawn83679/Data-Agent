-- Create email verification codes table
CREATE TABLE IF NOT EXISTS sys_email_verification_codes
(
    id           BIGSERIAL PRIMARY KEY,
    email        VARCHAR(255) NOT NULL,
    code         VARCHAR(10)  NOT NULL,
    code_type    VARCHAR(20)  NOT NULL,
    ip_address   VARCHAR(45),
    expires_at   TIMESTAMP    NOT NULL,
    verified     BOOLEAN DEFAULT FALSE,
    verified_at  TIMESTAMP,
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add comments
COMMENT ON TABLE sys_email_verification_codes IS 'Email verification codes table';
COMMENT ON COLUMN sys_email_verification_codes.id IS 'Primary key';
COMMENT ON COLUMN sys_email_verification_codes.email IS 'Email address';
COMMENT ON COLUMN sys_email_verification_codes.code IS 'Verification code';
COMMENT ON COLUMN sys_email_verification_codes.code_type IS 'Code type: LOGIN, REGISTER, RESET_PASSWORD';
COMMENT ON COLUMN sys_email_verification_codes.ip_address IS 'Request IP address';
COMMENT ON COLUMN sys_email_verification_codes.expires_at IS 'Expiration time';
COMMENT ON COLUMN sys_email_verification_codes.verified IS 'Whether verified (false: not verified, true: verified)';
COMMENT ON COLUMN sys_email_verification_codes.verified_at IS 'Verification time';
COMMENT ON COLUMN sys_email_verification_codes.create_time IS 'Create time';
COMMENT ON COLUMN sys_email_verification_codes.update_time IS 'Update time';

-- Create indexes
CREATE INDEX idx_email ON sys_email_verification_codes(email);
CREATE INDEX idx_code_type ON sys_email_verification_codes(code_type);
CREATE INDEX idx_expires_at ON sys_email_verification_codes(expires_at);
CREATE INDEX idx_create_time ON sys_email_verification_codes(create_time);
CREATE INDEX idx_email_code_type ON sys_email_verification_codes(email, code_type);
