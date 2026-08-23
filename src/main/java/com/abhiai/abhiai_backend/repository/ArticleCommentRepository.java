package com.abhiai.abhiai_backend.repository;
import java.util.Optional; import java.util.UUID;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable; import org.springframework.data.jpa.repository.*;
import com.abhiai.abhiai_backend.entity.ArticleComment;
public interface ArticleCommentRepository extends JpaRepository<ArticleComment,UUID>{
    @EntityGraph(attributePaths="author") Page<ArticleComment> findByArticleIdAndDeletedAtIsNull(UUID articleId,Pageable pageable);
    @EntityGraph(attributePaths={"author","article"}) Optional<ArticleComment> findByIdAndDeletedAtIsNull(UUID id);
}
