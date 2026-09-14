-- Additive: all existing conversations remain ordinary chats.
ALTER TABLE conversations ADD COLUMN character_assistant boolean NOT NULL DEFAULT false;
CREATE INDEX idx_character_assistant_user ON conversations(user_id, updated_at DESC)
    WHERE character_assistant = true;
ALTER TABLE messages ADD COLUMN client_item_id varchar(128);
CREATE UNIQUE INDEX idx_message_client_item ON messages(conversation_id, client_item_id)
    WHERE client_item_id IS NOT NULL;
