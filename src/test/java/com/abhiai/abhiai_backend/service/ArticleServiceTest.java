package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.abhiai.abhiai_backend.dto.article.*;
import com.abhiai.abhiai_backend.entity.*;
import com.abhiai.abhiai_backend.exception.*;
import com.abhiai.abhiai_backend.repository.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {
    @Mock ArticleRepository articles; @Mock ArticleLikeRepository likes; @Mock ArticleCommentRepository comments; @Mock UserRepository users;
    private ArticleService service; private User author; private Article article;
    private final UUID userId=UUID.randomUUID(), articleId=UUID.randomUUID();
    @BeforeEach void setup(){service=new ArticleService(articles,likes,comments,users);author=new User("writer","Writer","writer@example.com","hash");ReflectionTestUtils.setField(author,"id",userId);article=new Article(author,"Clean Architecture","A practical guide",null,"This is a sufficiently long article body explaining clean architecture principles.");ReflectionTestUtils.setField(article,"id",articleId);}
    @Test void createsSeparateLongFormArticle(){when(users.findById(userId)).thenReturn(Optional.of(author));when(articles.saveAndFlush(any())).thenAnswer(invocation->{Article value=invocation.getArgument(0);ReflectionTestUtils.setField(value,"id",articleId);return value;});ArticleResponse result=service.create(userId,new UpsertArticleRequest(" Clean Architecture "," A practical guide ",null," This is a sufficiently long article body explaining clean architecture principles. "));assertEquals(articleId,result.id());assertEquals("Clean Architecture",result.title());}
    @Test void rejectsUnsafeCoverScheme(){when(users.findById(userId)).thenReturn(Optional.of(author));assertThrows(InvalidArticleException.class,()->service.create(userId,new UpsertArticleRequest("Title","Summary","javascript:alert(1)","This is a sufficiently long article body that passes validation for this service test.")));}
    @Test void preventsDuplicateLike(){when(articles.findByIdAndDeletedAtIsNull(articleId)).thenReturn(Optional.of(article));when(users.findById(userId)).thenReturn(Optional.of(author));when(likes.existsByArticleIdAndUserId(articleId,userId)).thenReturn(true);assertThrows(DuplicateArticleLikeException.class,()->service.like(userId,articleId));verify(likes,never()).saveAndFlush(any());}
    @Test void preventsAnotherUserDeletingArticle(){UUID other=UUID.randomUUID();when(articles.findByIdAndDeletedAtIsNull(articleId)).thenReturn(Optional.of(article));assertThrows(UnauthorizedActionException.class,()->service.delete(other,articleId));verify(articles,never()).saveAndFlush(any());}
}
