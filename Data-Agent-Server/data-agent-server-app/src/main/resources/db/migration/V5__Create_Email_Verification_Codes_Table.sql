-- Create email verification codes table
CREATE TABLE IF NOT EXISTS email_verification_codes
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
COMMENT ON TABLE email_verification_codes IS 'Email verification codes table';
COMMENT ON COLUMN email_verification_codes.id IS 'Primary key';
COMMENT ON COLUMN email_verification_codes.email IS 'Email address';
COMMENT ON COLUMN email_verification_codes.code IS 'Verification code';
COMMENT ON COLUMN email_verification_codes.code_type IS 'Code type: LOGIN, REGISTER, RESET_PASSWORD';
COMMENT ON COLUMN email_verification_codes.ip_address IS 'Request IP address';
COMMENT ON COLUMN email_verification_codes.expires_at IS 'Expiration time';
COMMENT ON COLUMN email_verification_codes.verified IS 'Whether verified (false: not verified, true: verified)';
COMMENT ON COLUMN email_verification_codes.verified_at IS 'Verification time';
COMMENT ON COLUMN email_verification_codes.create_time IS 'Create time';
COMMENT ON COLUMN email_verification_codes.update_time IS 'Update time';

-- Create indexes
CREATE INDEX idx_email ON email_verification_codes(email);
CREATE INDEX idx_code_type ON email_verification_codes(code_type);
CREATE INDEX idx_expires_at ON email_verification_codes(expires_at);
CREATE INDEX idx_create_time ON email_verification_codes(create_time);
CREATE INDEX idx_email_code_type ON email_verification_codes(email, code_type);

-- Create trigger for update_time
CREATE OR REPLACE FUNCTION update_email_verification_codes_update_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_email_verification_codes_update_time
    BEFORE UPDATE ON email_verification_codes
    FOR EACH ROW
    EXECUTE FUNCTION update_email_verification_codes_update_time();
