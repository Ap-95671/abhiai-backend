CREATE TABLE creator_daily_metrics (
    id UUID PRIMARY KEY,
    creator_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    metric_date DATE NOT NULL,
    post_impressions BIGINT NOT NULL DEFAULT 0 CHECK (post_impressions >= 0),
    unique_post_viewers BIGINT NOT NULL DEFAULT 0 CHECK (unique_post_viewers >= 0),
    profile_views BIGINT NOT NULL DEFAULT 0 CHECK (profile_views >= 0),
    unique_profile_viewers BIGINT NOT NULL DEFAULT 0 CHECK (unique_profile_viewers >= 0),
    engagements BIGINT NOT NULL DEFAULT 0 CHECK (engagements >= 0),
    new_followers BIGINT NOT NULL DEFAULT 0 CHECK (new_followers >= 0),
    unfollows BIGINT NOT NULL DEFAULT 0 CHECK (unfollows >= 0),
    CONSTRAINT uq_creator_daily_metrics UNIQUE (creator_id, metric_date)
);

CREATE INDEX idx_creator_metrics_date ON creator_daily_metrics (creator_id, metric_date DESC);

CREATE TABLE post_daily_metrics (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    metric_date DATE NOT NULL,
    impressions BIGINT NOT NULL DEFAULT 0 CHECK (impressions >= 0),
    unique_viewers BIGINT NOT NULL DEFAULT 0 CHECK (unique_viewers >= 0),
    engagements BIGINT NOT NULL DEFAULT 0 CHECK (engagements >= 0),
    CONSTRAINT uq_post_daily_metrics UNIQUE (post_id, metric_date)
);

CREATE INDEX idx_post_metrics_date ON post_daily_metrics (post_id, metric_date DESC);

CREATE TABLE post_impression_uniques (
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    viewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    metric_date DATE NOT NULL,
    PRIMARY KEY (post_id, viewer_id, metric_date)
);

CREATE TABLE profile_view_uniques (
    profile_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    viewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    metric_date DATE NOT NULL,
    PRIMARY KEY (profile_id, viewer_id, metric_date)
);
