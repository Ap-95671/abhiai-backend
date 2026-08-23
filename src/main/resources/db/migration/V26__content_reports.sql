CREATE TABLE content_reports (
    id UUID PRIMARY KEY,
    reporter_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reported_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    target_type VARCHAR(24) NOT NULL,
    target_context VARCHAR(32),
    target_id UUID NOT NULL,
    reason VARCHAR(32) NOT NULL,
    details VARCHAR(1000),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT chk_report_target_type CHECK (target_type IN ('POST','USER','COMMENT','MESSAGE','COMMUNITY')),
    CONSTRAINT chk_report_target_context CHECK (target_context IS NULL OR target_context IN ('POST_REPLY','ARTICLE_COMMENT','DIRECT_MESSAGE','GROUP_MESSAGE')),
    CONSTRAINT chk_report_reason CHECK (reason IN ('SPAM','HARASSMENT','HATEFUL_CONTENT','VIOLENCE','NUDITY','IMPERSONATION','MISINFORMATION','OTHER')),
    CONSTRAINT chk_report_status CHECK (status IN ('PENDING','REVIEWING','RESOLVED','DISMISSED')),
    CONSTRAINT chk_report_context_shape CHECK (
      (target_type IN ('POST','USER','COMMUNITY') AND target_context IS NULL)
      OR (target_type = 'COMMENT' AND target_context IN ('POST_REPLY','ARTICLE_COMMENT'))
      OR (target_type = 'MESSAGE' AND target_context IN ('DIRECT_MESSAGE','GROUP_MESSAGE'))
    ),
    CONSTRAINT uq_content_report UNIQUE NULLS NOT DISTINCT (reporter_id,target_type,target_context,target_id)
);

CREATE INDEX idx_content_reports_status_created ON content_reports (status,created_at,id);
CREATE INDEX idx_content_reports_reporter_created ON content_reports (reporter_id,created_at DESC,id DESC);
CREATE INDEX idx_content_reports_target ON content_reports (target_type,target_context,target_id);
