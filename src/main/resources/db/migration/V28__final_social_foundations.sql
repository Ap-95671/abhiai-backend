ALTER TABLE users ADD COLUMN account_privacy VARCHAR(16) NOT NULL DEFAULT 'PUBLIC';
ALTER TABLE users ADD CONSTRAINT chk_users_account_privacy CHECK (account_privacy IN ('PUBLIC','PRIVATE'));

CREATE TABLE user_mutes (
 id UUID PRIMARY KEY, muter_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 muted_user_id UUID REFERENCES users(id) ON DELETE CASCADE, muted_term VARCHAR(100), mute_type VARCHAR(16) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT chk_user_mutes_type CHECK (mute_type IN ('USER','KEYWORD','HASHTAG')),
 CONSTRAINT chk_user_mutes_target CHECK ((mute_type='USER' AND muted_user_id IS NOT NULL AND muted_term IS NULL AND muter_id<>muted_user_id) OR (mute_type<>'USER' AND muted_user_id IS NULL AND muted_term IS NOT NULL)),
 CONSTRAINT uk_user_mutes_user UNIQUE NULLS NOT DISTINCT (muter_id, muted_user_id, mute_type),
 CONSTRAINT uk_user_mutes_term UNIQUE NULLS NOT DISTINCT (muter_id, muted_term, mute_type)
);
CREATE INDEX idx_user_mutes_muter ON user_mutes(muter_id, mute_type);

CREATE TABLE follow_requests (
 id UUID PRIMARY KEY, requester_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 target_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT chk_follow_requests_not_self CHECK (requester_id<>target_id),
 CONSTRAINT chk_follow_requests_status CHECK (status IN ('PENDING','ACCEPTED','REJECTED','CANCELLED')),
 CONSTRAINT uk_follow_requests_pair UNIQUE(requester_id,target_id)
);
CREATE INDEX idx_follow_requests_target_status ON follow_requests(target_id,status,created_at DESC);

CREATE TABLE moderation_audit_logs (
 id UUID PRIMARY KEY, administrator_email VARCHAR(254) NOT NULL, action VARCHAR(32) NOT NULL,
 target_type VARCHAR(32) NOT NULL, target_id UUID NOT NULL, reason VARCHAR(1000) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_moderation_audit_created ON moderation_audit_logs(created_at DESC);

CREATE TABLE recommendation_signals (
 id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 signal_type VARCHAR(32) NOT NULL, entity_type VARCHAR(24) NOT NULL, entity_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT chk_recommendation_signal CHECK (signal_type IN ('VIEW','LIKE','REPLY','REPOST','BOOKMARK','FOLLOW','PROFILE_VISIT','VIDEO_START','VIDEO_COMPLETE','HIDE','NOT_INTERESTED'))
);
CREATE INDEX idx_recommendation_signals_user_created ON recommendation_signals(user_id,created_at DESC);
CREATE INDEX idx_recommendation_signals_entity_created ON recommendation_signals(entity_type,entity_id,created_at DESC);
