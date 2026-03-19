CREATE TABLE IF NOT EXISTS ai_permission_rule (
    id BIGSERIAL PRIMARY KEY,
    scope_type VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    conversation_id BIGINT,
    connection_id BIGINT NOT NULL,
    catalog_name VARCHAR(255),
    catalog_match_mode VARCHAR(32) NOT NULL,
    schema_name VARCHAR(255),
    schema_match_mode VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_permission_rule IS 'Default-allow permission rules for AI write execution';
COMMENT ON COLUMN ai_permission_rule.scope_type IS 'Permission scope: USER or CONVERSATION';
COMMENT ON COLUMN ai_permission_rule.user_id IS 'Owner user id';
COMMENT ON COLUMN ai_permission_rule.conversation_id IS 'Conversation id for conversation-scoped permissions';
COMMENT ON COLUMN ai_permission_rule.connection_id IS 'Connection id bound to the permission';
COMMENT ON COLUMN ai_permission_rule.catalog_name IS 'Catalog/database name when catalog_match_mode is EXACT';
COMMENT ON COLUMN ai_permission_rule.catalog_match_mode IS 'Catalog match mode: EXACT or ANY';
COMMENT ON COLUMN ai_permission_rule.schema_name IS 'Schema name when schema_match_mode is EXACT';
COMMENT ON COLUMN ai_permission_rule.schema_match_mode IS 'Schema match mode: EXACT or ANY';
COMMENT ON COLUMN ai_permission_rule.enabled IS 'Whether the permission is enabled';

CREATE INDEX IF NOT EXISTS idx_ai_permission_rule_user_scope
    ON ai_permission_rule (user_id, scope_type, conversation_id);

CREATE INDEX IF NOT EXISTS idx_ai_permission_rule_match
    ON ai_permission_rule (
        user_id,
        scope_type,
        connection_id,
        catalog_match_mode,
        catalog_name,
        schema_match_mode,
        schema_name
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_ai_permission_rule_scope_match
    ON ai_permission_rule (
        user_id,
        scope_type,
        COALESCE(conversation_id, -1),
        connection_id,
        catalog_match_mode,
        COALESCE(catalog_name, '__NULL__'),
        schema_match_mode,
        COALESCE(schema_name, '__NULL__')
    );
