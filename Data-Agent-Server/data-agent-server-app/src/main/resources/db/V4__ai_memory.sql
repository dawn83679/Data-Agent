CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS ai_memory (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL,
    conversation_id         BIGINT,
    scope                   VARCHAR(32) NOT NULL DEFAULT 'USER',
    memory_type             VARCHAR(32) NOT NULL,
    sub_type                VARCHAR(64) NOT NULL,
    source_type             VARCHAR(32) NOT NULL DEFAULT 'AGENT',
    title                   VARCHAR(255),
    content                 TEXT NOT NULL,
    reason                  VARCHAR(512),
    enable                  SMALLINT NOT NULL DEFAULT 1,
    access_count            INT NOT NULL DEFAULT 0,
    last_accessed_at        TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_memory IS 'Primary durable memory table used by runtime recall, updateMemory, manual audit, and lifecycle maintenance';
COMMENT ON COLUMN ai_memory.user_id IS 'Owner user id';
COMMENT ON COLUMN ai_memory.conversation_id IS 'Conversation where the memory was last created or updated';
COMMENT ON COLUMN ai_memory.scope IS 'Memory scope: USER / CONVERSATION';
COMMENT ON COLUMN ai_memory.memory_type IS 'Top-level semantic memory type';
COMMENT ON COLUMN ai_memory.sub_type IS 'Refined subtype within the memory type';
COMMENT ON COLUMN ai_memory.source_type IS 'Source of memory creation, such as AGENT or MANUAL';
COMMENT ON COLUMN ai_memory.title IS 'Short human-readable title';
COMMENT ON COLUMN ai_memory.content IS 'Authoritative durable memory content';
COMMENT ON COLUMN ai_memory.reason IS 'Short explanation of why this memory should persist';
COMMENT ON COLUMN ai_memory.enable IS 'Memory availability flag: 1=ENABLE, 0=DISABLE';
COMMENT ON COLUMN ai_memory.access_count IS 'Number of times this memory has been retrieved';
COMMENT ON COLUMN ai_memory.last_accessed_at IS 'Last retrieval time';

CREATE INDEX IF NOT EXISTS idx_ai_memory_user_enable
    ON ai_memory (user_id, enable);

CREATE INDEX IF NOT EXISTS idx_ai_memory_user_scope_updated
    ON ai_memory (user_id, scope, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_memory_user_type_enable
    ON ai_memory (user_id, memory_type, enable);
