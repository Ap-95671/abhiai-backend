ALTER TABLE social_notifications
    DROP CONSTRAINT chk_social_notifications_type,
    DROP CONSTRAINT chk_social_notifications_post_requirement;

UPDATE social_notifications SET type = 'NEW_FOLLOWER' WHERE type = 'FOLLOW';
UPDATE social_notifications SET type = 'POST_LIKE' WHERE type = 'LIKE';
UPDATE social_notifications SET type = 'POST_REPLY' WHERE type = 'REPLY';
UPDATE social_notifications SET type = 'POST_REPOST' WHERE type = 'REPOST';

DELETE FROM social_notifications older
USING social_notifications newer
WHERE older.id <> newer.id
  AND older.recipient_id = newer.recipient_id
  AND older.actor_id = newer.actor_id
  AND older.type = newer.type
  AND COALESCE(older.post_id, '00000000-0000-0000-0000-000000000000'::UUID)
      = COALESCE(newer.post_id, '00000000-0000-0000-0000-000000000000'::UUID)
  AND older.type IN ('NEW_FOLLOWER', 'POST_LIKE', 'POST_REPOST', 'MENTION')
  AND (older.created_at < newer.created_at
       OR (older.created_at = newer.created_at AND older.id < newer.id));

ALTER TABLE social_notifications
    ADD CONSTRAINT chk_social_notifications_type
        CHECK (type IN ('NEW_FOLLOWER', 'POST_LIKE', 'POST_REPLY', 'POST_REPOST', 'MENTION')),
    ADD CONSTRAINT chk_social_notifications_post_requirement
        CHECK (
            (type = 'NEW_FOLLOWER' AND post_id IS NULL)
            OR (type IN ('POST_LIKE', 'POST_REPLY', 'POST_REPOST', 'MENTION') AND post_id IS NOT NULL)
        );

CREATE UNIQUE INDEX uq_social_notifications_idempotent_event
    ON social_notifications (
        recipient_id,
        actor_id,
        type,
        COALESCE(post_id, '00000000-0000-0000-0000-000000000000'::UUID))
    WHERE type IN ('NEW_FOLLOWER', 'POST_LIKE', 'POST_REPOST', 'MENTION');

CREATE INDEX idx_social_notifications_recipient_unread_page
    ON social_notifications (recipient_id, created_at DESC, id DESC)
    WHERE read_at IS NULL;
