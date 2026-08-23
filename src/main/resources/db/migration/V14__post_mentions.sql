CREATE TABLE post_mentions (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    mentioned_user_id UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_post_mentions_post
        FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_mentions_user
        FOREIGN KEY (mentioned_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_post_mentions_post_user UNIQUE (post_id, mentioned_user_id)
);

CREATE INDEX idx_post_mentions_user_created
    ON post_mentions (mentioned_user_id, created_at DESC, id DESC);

CREATE INDEX idx_post_mentions_post
    ON post_mentions (post_id);

INSERT INTO post_mentions (id, post_id, mentioned_user_id, created_at)
SELECT (
           SUBSTRING(MD5(post.id::TEXT || ':' || mentioned_user.id::TEXT), 1, 8) || '-' ||
           SUBSTRING(MD5(post.id::TEXT || ':' || mentioned_user.id::TEXT), 9, 4) || '-' ||
           SUBSTRING(MD5(post.id::TEXT || ':' || mentioned_user.id::TEXT), 13, 4) || '-' ||
           SUBSTRING(MD5(post.id::TEXT || ':' || mentioned_user.id::TEXT), 17, 4) || '-' ||
           SUBSTRING(MD5(post.id::TEXT || ':' || mentioned_user.id::TEXT), 21, 12)
       )::UUID,
       post.id,
       mentioned_user.id,
       post.created_at
FROM posts post
CROSS JOIN LATERAL REGEXP_MATCHES(
        post.text_content,
        '(^|[^[:alnum:]_])@([[:alnum:]_]{3,30})',
        'g') AS mention_match
JOIN users mentioned_user
  ON LOWER(mentioned_user.username) = LOWER(mention_match[2])
WHERE post.deleted_at IS NULL
ON CONFLICT (post_id, mentioned_user_id) DO NOTHING;

ALTER TABLE social_notifications
    DROP CONSTRAINT chk_social_notifications_type,
    DROP CONSTRAINT chk_social_notifications_post_requirement;

ALTER TABLE social_notifications
    ADD CONSTRAINT chk_social_notifications_type
        CHECK (type IN ('FOLLOW', 'LIKE', 'REPLY', 'REPOST', 'MENTION')),
    ADD CONSTRAINT chk_social_notifications_post_requirement
        CHECK (
            (type = 'FOLLOW' AND post_id IS NULL)
            OR (type IN ('LIKE', 'REPLY', 'REPOST', 'MENTION') AND post_id IS NOT NULL)
        );
