package com.abhiai.abhiai_backend.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.abhiai.abhiai_backend.entity.PostPoll;
import jakarta.persistence.LockModeType;

public interface PostPollRepository extends JpaRepository<PostPoll, UUID> {
    @EntityGraph(attributePaths = {"choices", "post", "post.author", "post.community"})
    Optional<PostPoll> findByPostId(UUID postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"choices", "post", "post.author", "post.community"})
    @Query("select poll from PostPoll poll where poll.post.id = :postId")
    Optional<PostPoll> findLockedByPostId(@Param("postId") UUID postId);
}
