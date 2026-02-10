-- ===============================================
-- Replace ai_message + ai_message_block with stored_chat_message
-- ===============================================

-- Drop old tables (indexes are dropped with the table)
DROP TABLE IF EXISTS ai_message_block;
DROP TABLE IF EXISTS ai_message;

-- ===============================================
-- ai_stored_chat_message Table
-- ===============================================

CREATE TABLE ai_stored_chat_message (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    token_count INTEGER DEFAULT 0,
    data TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_stored_chat_message IS 'Chat message table, one row per message';
COMMENT ON COLUMN ai_stored_chat_message.id IS 'Primary key';
COMMENT ON COLUMN ai_stored_chat_message.conversation_id IS 'Conversation ID';
COMMENT ON COLUMN ai_stored_chat_message.role IS 'Message role, e.g. AI, USER, SYSTEM, TOOL_EXECUTION_RESULT';
COMMENT ON COLUMN ai_stored_chat_message.token_count IS 'Token count';
COMMENT ON COLUMN ai_stored_chat_message.data IS 'Message content in JSON';
COMMENT ON COLUMN ai_stored_chat_message.created_at IS 'Created time';
COMMENT ON COLUMN ai_stored_chat_message.updated_at IS 'Updated time';

CREATE INDEX idx_ai_stored_chat_message_conversation_id ON ai_stored_chat_message(conversation_id);
CREATE INDEX idx_ai_stored_chat_message_conversation_created ON ai_stored_chat_message(conversation_id, created_at);
CREATE INDEX idx_ai_stored_chat_message_role ON ai_stored_chat_message(role);
