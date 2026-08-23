package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.abhiai.abhiai_backend.entity.Hashtag;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostHashtag;
import com.abhiai.abhiai_backend.entity.PostVisibility;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.InvalidHashtagException;
import com.abhiai.abhiai_backend.repository.HashtagRepository;
import com.abhiai.abhiai_backend.repository.PostHashtagRepository;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class HashtagServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID POST_ID = UUID.randomUUID();

    @Mock private HashtagRepository hashtagRepository;
    @Mock private PostHashtagRepository relationRepository;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;

    private HashtagService hashtagService;
    private Post post;

    @BeforeEach
    void setUp() {
        hashtagService = new HashtagService(
                hashtagRepository, relationRepository, postRepository, userRepository);
        User author = new User("author", "Author", "author@example.com", "hash");
        ReflectionTestUtils.setField(author, "id", USER_ID);
        post = new Post(author, "Post", PostVisibility.PUBLIC);
        ReflectionTestUtils.setField(post, "id", POST_ID);
    }

    @Test
    void extractsNormalizedUniqueTagsWhilePreservingFirstDisplayCase() {
        Map<String, String> tags = hashtagService.extract(
                "Building #AbhiAI with #Java and #abhiai. Ignore email#fragment and #__.");

        assertEquals(Map.of("abhiai", "AbhiAI", "java", "Java"), tags);
    }

    @Test
    void rejectsMoreThanTenUniqueHashtags() {
        assertThrows(InvalidHashtagException.class, () -> hashtagService.extract(
                "#a1 #a2 #a3 #a4 #a5 #a6 #a7 #a8 #a9 #a10 #a11"));
    }

    @Test
    void addsNewRelationsAndIncrementsCanonicalCounters() {
        Hashtag java = hashtag(UUID.randomUUID(), "java", "Java", 0);
        Hashtag spring = hashtag(UUID.randomUUID(), "spring", "Spring", 0);
        when(relationRepository.findAllByPostId(POST_ID)).thenReturn(List.of());
        when(hashtagRepository.findByNormalizedTag(anyString())).thenAnswer(invocation -> {
            String tag = invocation.getArgument(0);
            return Optional.of(tag.equals("java") ? java : spring);
        });

        hashtagService.synchronize(post, "Learning #Java and #Spring");

        verify(hashtagRepository).insertIfAbsent(any(UUID.class), eq("java"), eq("Java"));
        verify(hashtagRepository).insertIfAbsent(any(UUID.class), eq("spring"), eq("Spring"));
        verify(hashtagRepository).incrementPostCount(java.getId());
        verify(hashtagRepository).incrementPostCount(spring.getId());
        verify(relationRepository).flush();
    }

    @Test
    void removesObsoleteRelationsWithoutDuplicatingUnchangedTags() {
        Hashtag java = hashtag(UUID.randomUUID(), "java", "Java", 2);
        Hashtag spring = hashtag(UUID.randomUUID(), "spring", "Spring", 1);
        PostHashtag javaRelation = new PostHashtag(post, java);
        PostHashtag springRelation = new PostHashtag(post, spring);
        when(relationRepository.findAllByPostId(POST_ID)).thenReturn(List.of(javaRelation, springRelation));

        hashtagService.synchronize(post, "Still using #Java");

        verify(relationRepository).delete(springRelation);
        verify(hashtagRepository).decrementPostCount(spring.getId());
        verify(relationRepository, never()).delete(javaRelation);
        verify(hashtagRepository, never()).insertIfAbsent(any(), any(), any());
    }

    @Test
    void normalizesRequestedTagAndLoadsVisibilityAwarePosts() {
        Hashtag hashtag = hashtag(UUID.randomUUID(), "abhiai", "AbhiAI", 4);
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(hashtagRepository.findByNormalizedTag("abhiai")).thenReturn(Optional.of(hashtag));
        when(postRepository.findVisiblePostsByHashtag(eq(USER_ID), eq(hashtag.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post)));

        var response = hashtagService.posts(USER_ID, "#AbhiAI", PageRequest.of(0, 20));

        assertEquals(POST_ID, response.content().getFirst().id());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).findVisiblePostsByHashtag(eq(USER_ID), eq(hashtag.getId()), pageable.capture());
        assertEquals("createdAt: DESC,id: DESC", pageable.getValue().getSort().toString());
    }

    @Test
    void returnsOnlyHashtagsWithActivePostsInTrendingOrder() {
        Hashtag hashtag = hashtag(UUID.randomUUID(), "java", "Java", 5);
        when(hashtagRepository.findByPostCountGreaterThan(eq(0L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(hashtag)));

        var response = hashtagService.trending(PageRequest.of(0, 500));

        assertEquals("java", response.content().getFirst().normalizedTag());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(hashtagRepository).findByPostCountGreaterThan(eq(0L), pageable.capture());
        assertEquals(50, pageable.getValue().getPageSize());
        assertEquals("postCount: DESC,normalizedTag: ASC,id: ASC", pageable.getValue().getSort().toString());
    }

    private Hashtag hashtag(UUID id, String normalized, String display, long count) {
        Hashtag hashtag = new Hashtag(id, normalized, display);
        ReflectionTestUtils.setField(hashtag, "postCount", count);
        return hashtag;
    }
}
