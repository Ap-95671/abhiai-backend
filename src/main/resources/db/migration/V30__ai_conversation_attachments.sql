CREATE TABLE conversation_attachments (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    message_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    media_asset_id UUID NOT NULL UNIQUE REFERENCES media_assets(id) ON DELETE RESTRICT,
    kind VARCHAR(20) NOT NULL,
    processing_status VARCHAR(20) NOT NULL,
    extracted_text TEXT,
    processing_error VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    CONSTRAINT chk_conversation_attachment_kind
        CHECK (kind IN ('IMAGE', 'DOCUMENT')),
    CONSTRAINT chk_conversation_attachment_status
        CHECK (processing_status IN ('PENDING', 'READY', 'FAILED'))
);

CREATE INDEX idx_conversation_attachments_conversation_created
    ON conversation_attachments (conversation_id, created_at, id);

CREATE INDEX idx_conversation_attachments_message
    ON conversation_attachments (message_id)
    WHERE message_id IS NOT NULL;

CREATE INDEX idx_conversation_attachments_text
    ON conversation_attachments
    USING GIN (to_tsvector('simple', COALESCE(extracted_text, '')));
