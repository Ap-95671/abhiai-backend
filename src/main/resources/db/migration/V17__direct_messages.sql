CREATE TABLE direct_conversations (
    id UUID PRIMARY KEY,
    participant_key VARCHAR(73) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE TABLE direct_conversation_participants (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    user_id UUID NOT NULL,
    joined_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_direct_participant_conversation_user UNIQUE (conversation_id, user_id),
    CONSTRAINT fk_direct_participant_conversation
        FOREIGN KEY (conversation_id) REFERENCES direct_conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_direct_participant_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE direct_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    content VARCHAR(2000),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT chk_direct_message_content CHECK (content IS NOT NULL OR deleted_at IS NOT NULL),
    CONSTRAINT fk_direct_message_conversation
        FOREIGN KEY (conversation_id) REFERENCES direct_conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_direct_message_sender
        FOREIGN KEY (sender_id) REFERENCES users (id)
);

CREATE TABLE direct_message_read_receipts (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL,
    user_id UUID NOT NULL,
    read_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_direct_message_receipt_message_user UNIQUE (message_id, user_id),
    CONSTRAINT fk_direct_message_receipt_message
        FOREIGN KEY (message_id) REFERENCES direct_messages (id) ON DELETE CASCADE,
    CONSTRAINT fk_direct_message_receipt_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_direct_participants_user_conversation
    ON direct_conversation_participants (user_id, conversation_id);

CREATE INDEX idx_direct_messages_conversation_created
    ON direct_messages (conversation_id, created_at DESC, id DESC);

CREATE INDEX idx_direct_messages_sender_created
    ON direct_messages (sender_id, created_at DESC, id DESC);

CREATE INDEX idx_direct_receipts_user_message
    ON direct_message_read_receipts (user_id, message_id);
