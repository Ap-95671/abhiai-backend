CREATE TABLE refresh_sessions (
    token_hash VARCHAR(64) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_refresh_sessions_expiry ON refresh_sessions(expires_at);
CREATE INDEX idx_refresh_sessions_user ON refresh_sessions(user_id);
