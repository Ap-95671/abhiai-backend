CREATE TABLE stories (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    media_id UUID UNIQUE REFERENCES media_assets(id) ON DELETE RESTRICT,
    type VARCHAR(12) NOT NULL,
    text_content VARCHAR(500),
    background_color VARCHAR(7) NOT NULL DEFAULT '#263B80',
    view_count BIGINT NOT NULL DEFAULT 0 CHECK (view_count >= 0),
    reaction_count BIGINT NOT NULL DEFAULT 0 CHECK (reaction_count >= 0),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_story_type CHECK (type IN ('TEXT', 'IMAGE', 'VIDEO')),
    CONSTRAINT chk_story_content CHECK (text_content IS NOT NULL OR media_id IS NOT NULL),
    CONSTRAINT chk_story_background CHECK (background_color ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE INDEX idx_stories_active ON stories(expires_at DESC, created_at DESC, id DESC);
CREATE INDEX idx_stories_author_active ON stories(author_id, expires_at DESC, created_at DESC);

CREATE TABLE story_views (
    id UUID PRIMARY KEY,
    story_id UUID NOT NULL REFERENCES stories(id) ON DELETE CASCADE,
    viewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_story_views_story_viewer UNIQUE (story_id, viewer_id)
);

CREATE INDEX idx_story_views_viewer ON story_views(viewer_id, created_at DESC);

CREATE TABLE story_reactions (
    id UUID PRIMARY KEY,
    story_id UUID NOT NULL REFERENCES stories(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reaction VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_story_reactions_story_user UNIQUE (story_id, user_id)
);

CREATE INDEX idx_story_reactions_user ON story_reactions(user_id, created_at DESC);
