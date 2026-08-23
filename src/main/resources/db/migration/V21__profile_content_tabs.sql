ALTER TABLE users
    ADD COLUMN show_likes_on_profile BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_post_replies_author_created
    ON post_replies (author_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_post_likes_user_created
    ON post_likes (user_id, created_at DESC, id DESC);
