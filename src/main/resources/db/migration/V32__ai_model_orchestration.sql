ALTER TABLE conversations
    ADD COLUMN model_selection_mode VARCHAR(16) NOT NULL DEFAULT 'AUTO',
    ADD COLUMN preferred_model_id VARCHAR(160);

ALTER TABLE messages
    ADD COLUMN ai_provider VARCHAR(64),
    ADD COLUMN ai_model VARCHAR(160),
    ADD COLUMN input_tokens INTEGER,
    ADD COLUMN output_tokens INTEGER,
    ADD COLUMN latency_ms BIGINT,
    ADD COLUMN fallback_used BOOLEAN;

CREATE INDEX idx_messages_ai_provider_created_at ON messages(ai_provider, created_at);
