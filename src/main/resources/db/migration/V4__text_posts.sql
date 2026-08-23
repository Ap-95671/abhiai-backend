CREATE TABLE posts (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL,
    text_content TEXT NOT NULL,
    visibility VARCHAR(16) NOT NULL,
    reply_count BIGINT NOT NULL DEFAULT 0,
    like_count BIGINT NOT NULL DEFAULT 0,
    repost_count BIGINT NOT NULL DEFAULT 0,
    bookmark_count BIGINT NOT NULL DEFAULT 0,
    view_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT fk_posts_author
        FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT chk_posts_visibility
        CHECK (visibility IN ('PUBLIC', 'FOLLOWERS', 'PRIVATE')),
    CONSTRAINT chk_posts_reply_count CHECK (reply_count >= 0),
    CONSTRAINT chk_posts_like_count CHECK (like_count >= 0),
    CONSTRAINT chk_posts_repost_count CHECK (repost_count >= 0),
    CONSTRAINT chk_posts_bookmark_count CHECK (bookmark_count >= 0),
    CONSTRAINT chk_posts_view_count CHECK (view_count >= 0)
);

CREATE INDEX idx_posts_author_created
    ON posts (author_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_posts_visibility_created
    ON posts (visibility, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;
