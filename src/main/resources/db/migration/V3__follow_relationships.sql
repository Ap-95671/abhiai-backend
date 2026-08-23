CREATE TABLE user_follows (
    id UUID PRIMARY KEY,
    follower_id UUID NOT NULL,
    following_id UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_user_follows_follower_following
        UNIQUE (follower_id, following_id),
    CONSTRAINT chk_user_follows_not_self
        CHECK (follower_id <> following_id),
    CONSTRAINT fk_user_follows_follower
        FOREIGN KEY (follower_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_follows_following
        FOREIGN KEY (following_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_follows_follower_created
    ON user_follows (follower_id, created_at DESC, id DESC);

CREATE INDEX idx_user_follows_following_created
    ON user_follows (following_id, created_at DESC, id DESC);
