package com.abhiai.abhiai_backend.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.abhiai.abhiai_backend.entity.Article;

public interface ArticleRepository extends JpaRepository<Article,UUID> {
    @EntityGraph(attributePaths="author") Optional<Article> findByIdAndDeletedAtIsNull(UUID id);
    @EntityGraph(attributePaths="author") Page<Article> findByDeletedAtIsNull(Pageable pageable);
    @EntityGraph(attributePaths="author") Page<Article> findByAuthorIdAndDeletedAtIsNull(UUID authorId,Pageable pageable);
    @Modifying(clearAutomatically=true,flushAutomatically=true) @Query("update Article a set a.likeCount=a.likeCount+1 where a.id=:id and a.deletedAt is null") int incrementLikeCount(@Param("id") UUID id);
    @Modifying(clearAutomatically=true,flushAutomatically=true) @Query("update Article a set a.likeCount=case when a.likeCount>0 then a.likeCount-1 else 0 end where a.id=:id") int decrementLikeCount(@Param("id") UUID id);
    @Modifying(clearAutomatically=true,flushAutomatically=true) @Query("update Article a set a.commentCount=a.commentCount+1 where a.id=:id and a.deletedAt is null") int incrementCommentCount(@Param("id") UUID id);
    @Modifying(clearAutomatically=true,flushAutomatically=true) @Query("update Article a set a.commentCount=case when a.commentCount>0 then a.commentCount-1 else 0 end where a.id=:id") int decrementCommentCount(@Param("id") UUID id);
    @Modifying(clearAutomatically=true,flushAutomatically=true) @Query("update Article a set a.shareCount=a.shareCount+1 where a.id=:id and a.deletedAt is null") int incrementShareCount(@Param("id") UUID id);
}
