CREATE TABLE group_conversations (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    image_url VARCHAR(2048),
    owner_id UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_group_conversation_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE TABLE group_participants (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(16) NOT NULL,
    joined_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_group_participant_conversation_user UNIQUE (conversation_id, user_id),
    CONSTRAINT chk_group_participant_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    CONSTRAINT fk_group_participant_conversation
        FOREIGN KEY (conversation_id) REFERENCES group_conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_group_participant_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE group_invitations (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    inviter_id UUID NOT NULL,
    invitee_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    responded_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT uk_group_invitation_conversation_invitee UNIQUE (conversation_id, invitee_id),
    CONSTRAINT chk_group_invitation_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED')),
    CONSTRAINT chk_group_invitation_not_self CHECK (inviter_id <> invitee_id),
    CONSTRAINT fk_group_invitation_conversation
        FOREIGN KEY (conversation_id) REFERENCES group_conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_group_invitation_inviter FOREIGN KEY (inviter_id) REFERENCES users (id),
    CONSTRAINT fk_group_invitation_invitee FOREIGN KEY (invitee_id) REFERENCES users (id)
);

CREATE TABLE group_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    content VARCHAR(2000),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT chk_group_message_content CHECK (content IS NOT NULL OR deleted_at IS NOT NULL),
    CONSTRAINT fk_group_message_conversation
        FOREIGN KEY (conversation_id) REFERENCES group_conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_group_message_sender FOREIGN KEY (sender_id) REFERENCES users (id)
);

CREATE INDEX idx_group_participants_user_conversation
    ON group_participants (user_id, conversation_id);
CREATE INDEX idx_group_invitations_invitee_status
    ON group_invitations (invitee_id, status, created_at DESC);
CREATE INDEX idx_group_messages_conversation_created
    ON group_messages (conversation_id, created_at DESC, id DESC);
