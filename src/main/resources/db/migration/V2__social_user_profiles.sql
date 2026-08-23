ALTER TABLE users
    ADD COLUMN username VARCHAR(30),
    ADD COLUMN bio VARCHAR(160),
    ADD COLUMN profile_picture_url VARCHAR(2048),
    ADD COLUMN cover_picture_url VARCHAR(2048),
    ADD COLUMN location VARCHAR(100),
    ADD COLUMN website VARCHAR(2048),
    ADD COLUMN date_of_birth DATE,
    ADD COLUMN verified_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
    ADD COLUMN follower_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN following_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN post_count BIGINT NOT NULL DEFAULT 0;

UPDATE users
SET username = LEFT(
        CASE
            WHEN LENGTH(REGEXP_REPLACE(LOWER(SPLIT_PART(email, '@', 1)), '[^a-z0-9_]', '', 'g')) >= 3
                THEN REGEXP_REPLACE(LOWER(SPLIT_PART(email, '@', 1)), '[^a-z0-9_]', '', 'g')
            ELSE 'user'
        END,
        21
    ) || '_' || LEFT(REPLACE(id::text, '-', ''), 8);

ALTER TABLE users
    ALTER COLUMN username SET NOT NULL,
    ADD CONSTRAINT chk_users_verified_status
        CHECK (verified_status IN ('NONE', 'VERIFIED', 'ORGANIZATION', 'CREATOR', 'OFFICIAL')),
    ADD CONSTRAINT chk_users_follower_count CHECK (follower_count >= 0),
    ADD CONSTRAINT chk_users_following_count CHECK (following_count >= 0),
    ADD CONSTRAINT chk_users_post_count CHECK (post_count >= 0);

CREATE UNIQUE INDEX uk_users_username_lower ON users (LOWER(username));
