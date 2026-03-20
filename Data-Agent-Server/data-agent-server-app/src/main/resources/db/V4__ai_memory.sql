-- ===============================================
-- AI: Memory schema
-- Fresh schema for durable memory write, retrieval, audit, and maintenance.
-- ===============================================

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS ai_memory (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL,
    conversation_id         BIGINT,
    workspace_context_key   VARCHAR(255),
    workspace_level         VARCHAR(16),
    scope                   VARCHAR(32) NOT NULL DEFAULT 'USER',
    memory_type             VARCHAR(32) NOT NULL,
    sub_type                VARCHAR(64),
    source_type             VARCHAR(32) NOT NULL DEFAULT 'AGENT',
    title                   VARCHAR(255),
    content                 TEXT NOT NULL,
    normalized_content_key  VARCHAR(512) NOT NULL,
    reason                  VARCHAR(512),
    source_message_ids      TEXT NOT NULL DEFAULT '[]',
    detail_json             TEXT NOT NULL DEFAULT '{}',
    status                  SMALLINT NOT NULL DEFAULT 0,
    confidence_score        DOUBLE PRECISION NOT NULL DEFAULT 0.90,
    salience_score          DOUBLE PRECISION NOT NULL DEFAULT 0.60,
    access_count            INT NOT NULL DEFAULT 0,
    use_count               INT NOT NULL DEFAULT 0,
    last_accessed_at        TIMESTAMP,
    last_used_at            TIMESTAMP,
    expires_at              TIMESTAMP,
    archived_at             TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_memory IS 'Primary durable memory table used by runtime recall, writeMemory, manual audit, and lifecycle maintenance';
COMMENT ON COLUMN ai_memory.user_id IS 'Owner user id';
COMMENT ON COLUMN ai_memory.conversation_id IS 'Conversation where the memory was last created or updated';
COMMENT ON COLUMN ai_memory.workspace_context_key IS 'Resolved workspace binding such as GLOBAL, connectionId, connectionId:catalogName, or connectionId:catalogName:schemaName';
COMMENT ON COLUMN ai_memory.workspace_level IS 'Abstract workspace binding level chosen by the AI: GLOBAL / CONNECTION / CATALOG / SCHEMA';
COMMENT ON COLUMN ai_memory.scope IS 'Memory scope: USER / WORKSPACE / CONVERSATION';
COMMENT ON COLUMN ai_memory.memory_type IS 'Top-level semantic memory type';
COMMENT ON COLUMN ai_memory.sub_type IS 'Refined subtype within the memory type';
COMMENT ON COLUMN ai_memory.source_type IS 'Source of memory creation, such as AGENT or MANUAL';
COMMENT ON COLUMN ai_memory.title IS 'Short human-readable title';
COMMENT ON COLUMN ai_memory.content IS 'Authoritative durable memory content';
COMMENT ON COLUMN ai_memory.normalized_content_key IS 'Normalized dedupe key derived from content';
COMMENT ON COLUMN ai_memory.reason IS 'Short explanation of why this memory should persist';
COMMENT ON COLUMN ai_memory.source_message_ids IS 'JSON array string of source message ids used to justify the memory';
COMMENT ON COLUMN ai_memory.detail_json IS 'Structured extension payload stored as JSON string';
COMMENT ON COLUMN ai_memory.status IS 'Memory lifecycle status: 0=ACTIVE, 1=ARCHIVED, 2=HIDDEN';
COMMENT ON COLUMN ai_memory.confidence_score IS '0-1 confidence assigned to the memory';
COMMENT ON COLUMN ai_memory.salience_score IS '0-1 salience used for ranking and cleanup';
COMMENT ON COLUMN ai_memory.access_count IS 'Number of times this memory has been retrieved';
COMMENT ON COLUMN ai_memory.use_count IS 'Number of times this memory was actually consumed in runtime';
COMMENT ON COLUMN ai_memory.last_accessed_at IS 'Last retrieval time';
COMMENT ON COLUMN ai_memory.last_used_at IS 'Last actual runtime usage time';
COMMENT ON COLUMN ai_memory.expires_at IS 'Optional expiration time';
COMMENT ON COLUMN ai_memory.archived_at IS 'Archive timestamp';

CREATE INDEX IF NOT EXISTS idx_ai_memory_user_status
    ON ai_memory (user_id, status);

CREATE INDEX IF NOT EXISTS idx_ai_memory_user_scope_updated
    ON ai_memory (user_id, scope, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_memory_user_type_status
    ON ai_memory (user_id, memory_type, status);

CREATE INDEX IF NOT EXISTS idx_ai_memory_user_last_used
    ON ai_memory (user_id, last_used_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_ai_memory_active_dedupe
    ON ai_memory (
        user_id,
        scope,
        COALESCE(workspace_context_key, '__NULL__'),
        memory_type,
        COALESCE(sub_type, '__NULL__'),
        normalized_content_key
    )
    WHERE status = 0;
