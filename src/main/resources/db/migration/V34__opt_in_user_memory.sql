ALTER TABLE users ADD COLUMN ai_memory_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE user_memories (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_memories_user_updated ON user_memories(user_id, updated_at DESC);
