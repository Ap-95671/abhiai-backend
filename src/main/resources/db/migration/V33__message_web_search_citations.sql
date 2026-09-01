CREATE TABLE message_citations (
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    title VARCHAR(500) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    domain VARCHAR(255) NOT NULL,
    PRIMARY KEY (message_id, position)
);

CREATE INDEX idx_message_citations_message_id ON message_citations(message_id);
