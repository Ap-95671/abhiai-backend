CREATE INDEX idx_hashtags_normalized_search
    ON hashtags (LOWER(normalized_tag) text_pattern_ops);

CREATE INDEX idx_posts_active_created
    ON posts (created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_posts_active_author_created
    ON posts (author_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;
