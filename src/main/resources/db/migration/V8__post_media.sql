CREATE TABLE media_assets (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id UUID REFERENCES posts(id) ON DELETE CASCADE,
    storage_key VARCHAR(120) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    byte_size BIGINT NOT NULL CHECK (byte_size > 0),
    position SMALLINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_media_position CHECK (position IS NULL OR position BETWEEN 0 AND 3),
    CONSTRAINT uq_post_media_position UNIQUE (post_id, position)
);

CREATE INDEX idx_media_owner ON media_assets(owner_id);
CREATE INDEX idx_media_post ON media_assets(post_id, position);
