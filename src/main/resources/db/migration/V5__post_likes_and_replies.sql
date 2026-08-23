CREATE TABLE post_likes (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_post_likes_post_user UNIQUE (post_id, user_id),
    CONSTRAINT fk_post_likes_post
        FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_likes_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_post_likes_post_created
    ON post_likes (post_id, created_at DESC, id DESC);

CREATE TABLE post_replies (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    author_id UUID NOT NULL,
    text_content TEXT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT fk_post_replies_post
        FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_replies_author
        FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_post_replies_text_not_blank
        CHECK (length(btrim(text_content)) > 0)
);

CREATE INDEX idx_post_replies_post_created
    ON post_replies (post_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;
