package com.abhiai.abhiai_backend.repository;
import java.util.Optional; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.abhiai.abhiai_backend.entity.ArticleLike;
public interface ArticleLikeRepository extends JpaRepository<ArticleLike,UUID>{
    boolean existsByArticleIdAndUserId(UUID articleId,UUID userId);
    Optional<ArticleLike> findByArticleIdAndUserId(UUID articleId,UUID userId);
}
