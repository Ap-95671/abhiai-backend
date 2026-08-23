CREATE TABLE social_notifications (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    post_id UUID,
    read_at TIMESTAMP(6) WITH TIME ZONE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_social_notifications_recipient
        FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_social_notifications_actor
        FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_social_notifications_post
        FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT chk_social_notifications_not_self
        CHECK (recipient_id <> actor_id),
    CONSTRAINT chk_social_notifications_type
        CHECK (type IN ('FOLLOW', 'LIKE', 'REPLY', 'REPOST')),
    CONSTRAINT chk_social_notifications_post_requirement
        CHECK (
            (type = 'FOLLOW' AND post_id IS NULL)
            OR (type IN ('LIKE', 'REPLY', 'REPOST') AND post_id IS NOT NULL)
        )
);

CREATE INDEX idx_social_notifications_recipient_created
    ON social_notifications (recipient_id, created_at DESC, id DESC);

CREATE INDEX idx_social_notifications_recipient_unread
    ON social_notifications (recipient_id, created_at DESC, id DESC)
    WHERE read_at IS NULL;

ALTER TABLE posts
    ADD COLUMN search_vector TSVECTOR
        GENERATED ALWAYS AS (
            TO_TSVECTOR('simple', COALESCE(text_content, ''))
        ) STORED;

CREATE INDEX idx_posts_search_vector
    ON posts USING GIN (search_vector)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_users_username_search
    ON users (LOWER(username) text_pattern_ops);

CREATE INDEX idx_users_display_name_search
    ON users (LOWER(display_name) text_pattern_ops);
