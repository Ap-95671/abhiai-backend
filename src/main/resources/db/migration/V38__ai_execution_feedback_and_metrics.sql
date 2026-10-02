-- Additive AI-only metadata; existing conversations/messages remain valid.
ALTER TABLE messages ADD COLUMN ai_request_id VARCHAR(64);
ALTER TABLE messages ADD COLUMN ai_intent VARCHAR(32);
ALTER TABLE messages ADD COLUMN ai_task_type VARCHAR(32);
ALTER TABLE messages ADD COLUMN ai_complexity VARCHAR(16);
ALTER TABLE messages ADD COLUMN ai_strategy VARCHAR(40);
ALTER TABLE messages ADD COLUMN ai_quality_score INTEGER;
ALTER TABLE messages ADD COLUMN ai_feedback BOOLEAN;
CREATE INDEX idx_messages_ai_request_id ON messages(ai_request_id) WHERE ai_request_id IS NOT NULL;

CREATE TABLE ai_routing_metrics (
    provider VARCHAR(64) NOT NULL,
    model VARCHAR(160) NOT NULL,
    intent VARCHAR(32) NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    strategy VARCHAR(40) NOT NULL,
    attempts BIGINT NOT NULL DEFAULT 0,
    successes BIGINT NOT NULL DEFAULT 0,
    latency_ms BIGINT NOT NULL DEFAULT 0,
    input_tokens BIGINT NOT NULL DEFAULT 0,
    output_tokens BIGINT NOT NULL DEFAULT 0,
    positive_feedback BIGINT NOT NULL DEFAULT 0,
    negative_feedback BIGINT NOT NULL DEFAULT 0,
    fallbacks BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY(provider, model, intent, task_type, strategy)
);
CREATE INDEX idx_ai_routing_metrics_task ON ai_routing_metrics(provider, model, task_type);

ALTER TABLE message_citations ADD COLUMN description VARCHAR(1600);
ALTER TABLE message_citations ADD COLUMN source_date VARCHAR(80);
ALTER TABLE message_citations ADD COLUMN retrieved_at TIMESTAMP WITH TIME ZONE;
