CREATE TABLE post_polls (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    expires_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    total_votes BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_post_polls_post UNIQUE (post_id),
    CONSTRAINT chk_post_polls_total_votes CHECK (total_votes >= 0),
    CONSTRAINT fk_post_polls_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE
);

CREATE TABLE poll_choices (
    id UUID PRIMARY KEY,
    poll_id UUID NOT NULL,
    choice_text VARCHAR(100) NOT NULL,
    position SMALLINT NOT NULL,
    vote_count BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_poll_choices_poll_position UNIQUE (poll_id, position),
    CONSTRAINT uk_poll_choices_id_poll UNIQUE (id, poll_id),
    CONSTRAINT chk_poll_choices_position CHECK (position BETWEEN 0 AND 3),
    CONSTRAINT chk_poll_choices_vote_count CHECK (vote_count >= 0),
    CONSTRAINT fk_poll_choices_poll FOREIGN KEY (poll_id) REFERENCES post_polls (id) ON DELETE CASCADE
);

CREATE TABLE poll_votes (
    id UUID PRIMARY KEY,
    poll_id UUID NOT NULL,
    choice_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_poll_votes_poll_user UNIQUE (poll_id, user_id),
    CONSTRAINT fk_poll_votes_poll FOREIGN KEY (poll_id) REFERENCES post_polls (id) ON DELETE CASCADE,
    CONSTRAINT fk_poll_votes_choice_poll FOREIGN KEY (choice_id, poll_id)
        REFERENCES poll_choices (id, poll_id) ON DELETE CASCADE,
    CONSTRAINT fk_poll_votes_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_poll_votes_user_created ON poll_votes (user_id, created_at DESC);
