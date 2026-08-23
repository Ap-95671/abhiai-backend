package com.abhiai.abhiai_backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.abhiai.abhiai_backend.entity.PostHashtag;

public interface PostHashtagRepository extends JpaRepository<PostHashtag, UUID> {
    @EntityGraph(attributePaths = "hashtag")
    List<PostHashtag> findAllByPostId(UUID postId);
}
