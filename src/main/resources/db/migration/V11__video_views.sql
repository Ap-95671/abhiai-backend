CREATE TABLE post_views (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    viewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_post_views_post_viewer UNIQUE (post_id, viewer_id)
);

CREATE INDEX idx_post_views_viewer_created
    ON post_views(viewer_id, created_at DESC);

CREATE INDEX idx_video_media_posts
    ON media_assets(post_id, content_type)
    WHERE content_type IN ('video/mp4', 'video/webm');
