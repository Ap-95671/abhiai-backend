CREATE TABLE articles (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    summary VARCHAR(320) NOT NULL,
    cover_image_url VARCHAR(2048),
    content TEXT NOT NULL,
    like_count BIGINT NOT NULL DEFAULT 0 CHECK (like_count >= 0),
    comment_count BIGINT NOT NULL DEFAULT 0 CHECK (comment_count >= 0),
    share_count BIGINT NOT NULL DEFAULT 0 CHECK (share_count >= 0),
    published_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_article_title_length CHECK (char_length(btrim(title)) BETWEEN 3 AND 180),
    CONSTRAINT chk_article_summary_length CHECK (char_length(btrim(summary)) BETWEEN 1 AND 320),
    CONSTRAINT chk_article_content_length CHECK (char_length(btrim(content)) BETWEEN 50 AND 50000)
);

CREATE INDEX idx_articles_published ON articles (published_at DESC, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_articles_author_published ON articles (author_id, published_at DESC, id DESC) WHERE deleted_at IS NULL;

CREATE TABLE article_likes (
    id UUID PRIMARY KEY,
    article_id UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_article_like UNIQUE (article_id, user_id)
);

CREATE INDEX idx_article_likes_article_created ON article_likes (article_id, created_at DESC);

CREATE TABLE article_comments (
    id UUID PRIMARY KEY,
    article_id UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_article_comment_content CHECK (char_length(btrim(content)) BETWEEN 1 AND 2000)
);

CREATE INDEX idx_article_comments_article_created ON article_comments (article_id, created_at ASC, id ASC) WHERE deleted_at IS NULL;
