CREATE TABLE IF NOT EXISTS ai_conversation_memory_cursor (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    last_processed_message_id BIGINT,
    last_processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_ai_conversation_memory_cursor UNIQUE (conversation_id)
);

CREATE INDEX IF NOT EXISTS idx_ai_conversation_memory_cursor_user_id
    ON ai_conversation_memory_cursor (user_id);
