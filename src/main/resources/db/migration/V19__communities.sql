CREATE TABLE communities (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(64) NOT NULL,
    description VARCHAR(1000),
    icon_url VARCHAR(2048),
    banner_url VARCHAR(2048),
    owner_id UUID NOT NULL,
    member_count BIGINT NOT NULL DEFAULT 1,
    privacy VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_communities_slug UNIQUE (slug),
    CONSTRAINT chk_communities_member_count CHECK (member_count >= 1),
    CONSTRAINT chk_communities_privacy CHECK (privacy IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT fk_communities_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE TABLE community_memberships (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(16) NOT NULL,
    joined_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_community_memberships_community_user UNIQUE (community_id, user_id),
    CONSTRAINT chk_community_memberships_role CHECK (role IN ('OWNER', 'MEMBER')),
    CONSTRAINT fk_community_memberships_community
        FOREIGN KEY (community_id) REFERENCES communities (id) ON DELETE CASCADE,
    CONSTRAINT fk_community_memberships_user FOREIGN KEY (user_id) REFERENCES users (id)
);

ALTER TABLE posts
    ADD COLUMN community_id UUID,
    ADD CONSTRAINT fk_posts_community
        FOREIGN KEY (community_id) REFERENCES communities (id) ON DELETE SET NULL;

CREATE INDEX idx_communities_discovery
    ON communities (privacy, member_count DESC, created_at DESC, id DESC);
CREATE INDEX idx_community_memberships_user
    ON community_memberships (user_id, joined_at DESC, community_id);
CREATE INDEX idx_posts_community_created
    ON posts (community_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;
