package com.abhiai.abhiai_backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.abhiai.abhiai_backend.entity.PostMention;

public interface PostMentionRepository extends JpaRepository<PostMention, UUID> {

    @EntityGraph(attributePaths = "mentionedUser")
    List<PostMention> findAllByPostId(UUID postId);
}
