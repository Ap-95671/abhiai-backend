CREATE TABLE post_reposts (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_post_reposts_post_user UNIQUE (post_id, user_id),
    CONSTRAINT fk_post_reposts_post
        FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_reposts_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_post_reposts_post_created
    ON post_reposts (post_id, created_at DESC, id DESC);

CREATE INDEX idx_post_reposts_user_created
    ON post_reposts (user_id, created_at DESC, id DESC);

CREATE TABLE post_bookmarks (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_post_bookmarks_post_user UNIQUE (post_id, user_id),
    CONSTRAINT fk_post_bookmarks_post
        FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_bookmarks_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_post_bookmarks_user_created
    ON post_bookmarks (user_id, created_at DESC, id DESC);
