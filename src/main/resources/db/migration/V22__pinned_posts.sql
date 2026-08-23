ALTER TABLE posts
    ADD COLUMN pinned_at TIMESTAMP(6) WITH TIME ZONE;

CREATE UNIQUE INDEX uk_posts_one_pin_per_author
    ON posts (author_id)
    WHERE pinned_at IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX idx_posts_profile_pinned
    ON posts (author_id, pinned_at DESC, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;
