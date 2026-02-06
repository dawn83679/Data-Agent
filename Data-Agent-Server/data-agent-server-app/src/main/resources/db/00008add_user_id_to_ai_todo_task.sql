-- Add user_id column to ai_todo_task table
ALTER TABLE ai_todo_task ADD COLUMN user_id BIGINT NOT NULL;

-- Add comment for the new column
COMMENT ON COLUMN ai_todo_task.user_id IS 'Associated user ID';

-- Add index for user_id
CREATE INDEX idx_ai_todo_task_user_id ON ai_todo_task(user_id);
