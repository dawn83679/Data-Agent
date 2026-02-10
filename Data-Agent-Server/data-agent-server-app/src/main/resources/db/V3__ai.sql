-- ===============================================
-- AI: Conversation and Chat
-- Tables: ai_conversation, ai_stored_chat_message
-- ===============================================

CREATE TABLE IF NOT EXISTS ai_conversation (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    title       VARCHAR(255),
    token_count INTEGER DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_conversation IS 'AI assistant conversation table';
COMMENT ON COLUMN ai_conversation.id IS 'Primary key ID for conversation';
COMMENT ON COLUMN ai_conversation.user_id IS 'Associated user ID';
COMMENT ON COLUMN ai_conversation.title IS 'Conversation title, can be generated from first message or customized by user';
COMMENT ON COLUMN ai_conversation.token_count IS 'Token usage statistics';
COMMENT ON COLUMN ai_conversation.created_at IS 'Created time';
COMMENT ON COLUMN ai_conversation.updated_at IS 'Updated time';

CREATE INDEX IF NOT EXISTS idx_ai_conversation_user_id ON ai_conversation (user_id);

-- ===============================================
-- ai_stored_chat_message
-- ===============================================

CREATE TABLE IF NOT EXISTS ai_stored_chat_message (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role            VARCHAR(32) NOT NULL,
    token_count     INTEGER DEFAULT 0,
    data            TEXT NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_stored_chat_message IS 'Chat message table, one row per message';
COMMENT ON COLUMN ai_stored_chat_message.id IS 'Primary key';
COMMENT ON COLUMN ai_stored_chat_message.conversation_id IS 'Conversation ID';
COMMENT ON COLUMN ai_stored_chat_message.role IS 'Message role, e.g. AI, USER, SYSTEM, TOOL_EXECUTION_RESULT';
COMMENT ON COLUMN ai_stored_chat_message.token_count IS 'Token count';
COMMENT ON COLUMN ai_stored_chat_message.data IS 'Message content in JSON';
COMMENT ON COLUMN ai_stored_chat_message.created_at IS 'Created time';
COMMENT ON COLUMN ai_stored_chat_message.updated_at IS 'Updated time';

CREATE INDEX IF NOT EXISTS idx_ai_stored_chat_message_conversation_id ON ai_stored_chat_message (conversation_id);
CREATE INDEX IF NOT EXISTS idx_ai_stored_chat_message_conversation_created ON ai_stored_chat_message (conversation_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_stored_chat_message_role ON ai_stored_chat_message (role);
