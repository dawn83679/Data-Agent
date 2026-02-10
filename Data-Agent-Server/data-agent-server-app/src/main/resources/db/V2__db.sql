-- ===============================================
-- Database: Connection Management
-- Table: db_connections
-- ===============================================

CREATE TABLE IF NOT EXISTS db_connections (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT,
    name           VARCHAR(100) NOT NULL UNIQUE,
    db_type        VARCHAR(20) NOT NULL,
    host           VARCHAR(255) NOT NULL,
    port           INTEGER NOT NULL,
    database       VARCHAR(100),
    username       VARCHAR(100),
    password       VARCHAR(255),
    driver_jar_path VARCHAR(500) NOT NULL,
    timeout        INTEGER DEFAULT 30,
    properties     TEXT DEFAULT '',
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE db_connections IS 'Database connection information table';
COMMENT ON COLUMN db_connections.id IS 'Primary key';
COMMENT ON COLUMN db_connections.user_id IS 'Associated system user ID, references sys_users.id';
COMMENT ON COLUMN db_connections.name IS 'Connection name, must be unique';
COMMENT ON COLUMN db_connections.db_type IS 'Database type: mysql, postgresql, oracle, redis, etc.';
COMMENT ON COLUMN db_connections.host IS 'Host address';
COMMENT ON COLUMN db_connections.port IS 'Port number';
COMMENT ON COLUMN db_connections.database IS 'Database name';
COMMENT ON COLUMN db_connections.username IS 'Database username';
COMMENT ON COLUMN db_connections.password IS 'Database password (encrypted storage)';
COMMENT ON COLUMN db_connections.driver_jar_path IS 'Path to external JDBC driver JAR file';
COMMENT ON COLUMN db_connections.timeout IS 'Connection timeout in seconds (default 30)';
COMMENT ON COLUMN db_connections.properties IS 'Connection properties in JSON format (passwords, ssl settings, etc.)';
COMMENT ON COLUMN db_connections.created_at IS 'Creation time';
COMMENT ON COLUMN db_connections.updated_at IS 'Update time';

CREATE INDEX IF NOT EXISTS idx_db_connections_db_type ON db_connections (db_type);
CREATE INDEX IF NOT EXISTS idx_db_connections_host_port ON db_connections (host, port);
CREATE INDEX IF NOT EXISTS idx_db_connections_user_id ON db_connections (user_id);
