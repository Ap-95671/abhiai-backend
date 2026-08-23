ALTER TABLE users ADD COLUMN profile_media_id UUID REFERENCES media_assets(id) ON DELETE SET NULL;
ALTER TABLE users ADD COLUMN cover_media_id UUID REFERENCES media_assets(id) ON DELETE SET NULL;
CREATE INDEX idx_users_profile_media ON users(profile_media_id);
CREATE INDEX idx_users_cover_media ON users(cover_media_id);
