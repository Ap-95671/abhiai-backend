package com.abhiai.abhiai_backend.service;

import java.net.URI;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.abhiai.abhiai_backend.dto.article.*;
import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.entity.*;
import com.abhiai.abhiai_backend.exception.*;
import com.abhiai.abhiai_backend.repository.*;

@Service
public class ArticleService {
    private static final int MAX_PAGE_SIZE=50;
    private static final Sort ARTICLE_SORT=Sort.by(Sort.Order.desc("publishedAt"),Sort.Order.desc("id"));
    private static final Sort COMMENT_SORT=Sort.by(Sort.Order.asc("createdAt"),Sort.Order.asc("id"));
    private final ArticleRepository articles; private final ArticleLikeRepository likes;
    private final ArticleCommentRepository comments; private final UserRepository users;
    public ArticleService(ArticleRepository articles,ArticleLikeRepository likes,ArticleCommentRepository comments,UserRepository users){this.articles=articles;this.likes=likes;this.comments=comments;this.users=users;}

    @Transactional
    public ArticleResponse create(UUID userId,UpsertArticleRequest request){
        User author=users.findById(userId).orElseThrow(UserNotFoundException::new);
        Article saved=articles.saveAndFlush(new Article(author,normalizeTitle(request.title()),normalizeSummary(request.summary()),normalizeCover(request.coverImageUrl()),normalizeContent(request.content())));
        return ArticleResponse.from(saved,false);
    }
    @Transactional(readOnly=true)
    public PageResponse<ArticleResponse> list(UUID userId,UUID authorId,Pageable pageable){
        Pageable normalized=normalize(pageable,ARTICLE_SORT); Page<Article> page=authorId==null?articles.findByDeletedAtIsNull(normalized):articles.findByAuthorIdAndDeletedAtIsNull(authorId,normalized);
        return PageResponse.from(page,a->ArticleResponse.from(a,likes.existsByArticleIdAndUserId(a.getId(),userId)));
    }
    @Transactional(readOnly=true) public ArticleResponse get(UUID userId,UUID articleId){Article a=find(articleId);return ArticleResponse.from(a,likes.existsByArticleIdAndUserId(articleId,userId));}
    @Transactional public ArticleResponse update(UUID userId,UUID articleId,UpsertArticleRequest request){Article a=find(articleId);requireAuthor(userId,a);a.update(normalizeTitle(request.title()),normalizeSummary(request.summary()),normalizeCover(request.coverImageUrl()),normalizeContent(request.content()));return ArticleResponse.from(articles.saveAndFlush(a),likes.existsByArticleIdAndUserId(articleId,userId));}
    @Transactional public void delete(UUID userId,UUID articleId){Article a=find(articleId);requireAuthor(userId,a);a.softDelete();articles.saveAndFlush(a);}
    @Transactional public ArticleResponse like(UUID userId,UUID articleId){Article a=find(articleId);User user=users.findById(userId).orElseThrow(UserNotFoundException::new);if(likes.existsByArticleIdAndUserId(articleId,userId))throw new DuplicateArticleLikeException();try{likes.saveAndFlush(new ArticleLike(a,user));}catch(DataIntegrityViolationException e){throw new DuplicateArticleLikeException();}articles.incrementLikeCount(articleId);return ArticleResponse.from(find(articleId),true);}
    @Transactional public ArticleResponse unlike(UUID userId,UUID articleId){find(articleId);ArticleLike like=likes.findByArticleIdAndUserId(articleId,userId).orElseThrow(()->new InvalidArticleException("You have not liked this article"));likes.delete(like);likes.flush();articles.decrementLikeCount(articleId);return ArticleResponse.from(find(articleId),false);}
    @Transactional(readOnly=true) public PageResponse<ArticleCommentResponse> comments(UUID articleId,Pageable pageable){find(articleId);return PageResponse.from(comments.findByArticleIdAndDeletedAtIsNull(articleId,normalize(pageable,COMMENT_SORT)),ArticleCommentResponse::from);}
    @Transactional public ArticleCommentResponse comment(UUID userId,UUID articleId,CreateArticleCommentRequest request){Article a=find(articleId);User author=users.findById(userId).orElseThrow(UserNotFoundException::new);String content=request.content().trim();if(content.isEmpty())throw new InvalidArticleException("Comment is required");ArticleComment saved=comments.saveAndFlush(new ArticleComment(a,author,content));articles.incrementCommentCount(articleId);return ArticleCommentResponse.from(saved);}
    @Transactional public void deleteComment(UUID userId,UUID articleId,UUID commentId){find(articleId);ArticleComment c=comments.findByIdAndDeletedAtIsNull(commentId).filter(item->item.getArticle().getId().equals(articleId)).orElseThrow(()->new InvalidArticleException("Comment not found"));if(!c.getAuthor().getId().equals(userId))throw new UnauthorizedActionException("Only the comment author can delete this comment");c.softDelete();comments.saveAndFlush(c);articles.decrementCommentCount(articleId);}
    @Transactional public ArticleShareResponse share(UUID articleId){find(articleId);articles.incrementShareCount(articleId);Article refreshed=find(articleId);return new ArticleShareResponse(articleId,refreshed.getShareCount());}

    private Article find(UUID id){return articles.findByIdAndDeletedAtIsNull(id).orElseThrow(ArticleNotFoundException::new);}
    private void requireAuthor(UUID userId,Article a){if(!a.getAuthor().getId().equals(userId))throw new UnauthorizedActionException("Only the author can modify this article");}
    private String normalizeTitle(String value){return value.trim();} private String normalizeSummary(String value){return value.trim();} private String normalizeContent(String value){return value.trim();}
    private String normalizeCover(String value){if(value==null||value.isBlank())return null;String normalized=value.trim();try{URI uri=URI.create(normalized);if(!"http".equalsIgnoreCase(uri.getScheme())&&!"https".equalsIgnoreCase(uri.getScheme()))throw new IllegalArgumentException();return normalized;}catch(IllegalArgumentException e){throw new InvalidArticleException("Cover image must be a valid HTTP or HTTPS URL");}}
    private Pageable normalize(Pageable p,Sort sort){return PageRequest.of(Math.max(0,p.getPageNumber()),Math.max(1,Math.min(p.getPageSize(),MAX_PAGE_SIZE)),sort);}
}
