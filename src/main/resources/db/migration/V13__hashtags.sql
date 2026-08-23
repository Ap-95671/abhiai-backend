CREATE TABLE hashtags (
    id UUID PRIMARY KEY,
    normalized_tag VARCHAR(50) NOT NULL UNIQUE,
    display_tag VARCHAR(50) NOT NULL,
    post_count BIGINT NOT NULL DEFAULT 0 CHECK (post_count >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE post_hashtags (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    hashtag_id UUID NOT NULL REFERENCES hashtags(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_post_hashtags_post_tag UNIQUE (post_id, hashtag_id)
);

CREATE INDEX idx_hashtags_trending ON hashtags(post_count DESC, normalized_tag);
CREATE INDEX idx_post_hashtags_tag_post ON post_hashtags(hashtag_id, post_id);

WITH extracted AS (
    SELECT post.id AS post_id, match[2] AS display_tag, lower(match[2]) AS normalized_tag
    FROM posts post
    CROSS JOIN LATERAL regexp_matches(post.text_content, '(^|[^[:alnum:]_])#([[:alnum:]_]{1,50})', 'g') AS match
    WHERE post.deleted_at IS NULL
), distinct_tags AS (
    SELECT normalized_tag, min(display_tag) AS display_tag
    FROM extracted
    GROUP BY normalized_tag
)
INSERT INTO hashtags(id, normalized_tag, display_tag, post_count, created_at)
SELECT md5(normalized_tag)::uuid, normalized_tag, display_tag, 0, CURRENT_TIMESTAMP
FROM distinct_tags
ON CONFLICT (normalized_tag) DO NOTHING;

WITH extracted AS (
    SELECT DISTINCT post.id AS post_id, lower(match[2]) AS normalized_tag
    FROM posts post
    CROSS JOIN LATERAL regexp_matches(post.text_content, '(^|[^[:alnum:]_])#([[:alnum:]_]{1,50})', 'g') AS match
    WHERE post.deleted_at IS NULL
)
INSERT INTO post_hashtags(id, post_id, hashtag_id, created_at)
SELECT md5(extracted.post_id::text || ':' || hashtag.id::text)::uuid,
       extracted.post_id,
       hashtag.id,
       CURRENT_TIMESTAMP
FROM extracted
JOIN hashtags hashtag ON hashtag.normalized_tag = extracted.normalized_tag
ON CONFLICT (post_id, hashtag_id) DO NOTHING;

UPDATE hashtags hashtag
SET post_count = (
    SELECT count(*) FROM post_hashtags relation WHERE relation.hashtag_id = hashtag.id
);
