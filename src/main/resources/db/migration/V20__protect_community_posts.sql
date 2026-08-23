ALTER TABLE posts
    DROP CONSTRAINT fk_posts_community,
    ADD CONSTRAINT fk_posts_community
        FOREIGN KEY (community_id) REFERENCES communities (id) ON DELETE RESTRICT;
