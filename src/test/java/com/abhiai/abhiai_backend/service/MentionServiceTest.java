package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostMention;
import com.abhiai.abhiai_backend.entity.PostVisibility;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.InvalidPostException;
import com.abhiai.abhiai_backend.repository.PostMentionRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class MentionServiceTest {

    private static final UUID POST_ID = UUID.randomUUID();
    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID MENTIONED_ID = UUID.randomUUID();

    @Mock private PostMentionRepository mentionRepository;
    @Mock private UserRepository userRepository;
    @Mock private PostAccessService postAccessService;
    @Mock private SocialNotificationService notificationService;

    private MentionService mentionService;
    private User author;
    private User mentioned;
    private Post post;

    @BeforeEach
    void setUp() {
        mentionService = new MentionService(
                mentionRepository, userRepository, postAccessService, notificationService);
        author = user(AUTHOR_ID, "author");
        mentioned = user(MENTIONED_ID, "abhiai");
        post = new Post(author, "Working on @abhiai", PostVisibility.PUBLIC);
        ReflectionTestUtils.setField(post, "id", POST_ID);
    }

    @Test
    void extractsNormalizedUniqueMentionsAtUsernameBoundaries() {
        assertEquals(
                Set.of("abhiai", "builder_2"),
                mentionService.extract("Hi @AbhiAI and @abhiai with @builder_2, not mail@test.com"));
    }

    @Test
    void rejectsMoreThanTwentyUniqueMentions() {
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < 21; index++) text.append(" @user_").append(index);

        assertThrows(InvalidPostException.class, () -> mentionService.extract(text.toString()));
    }

    @Test
    void addsOnlyResolvableMentionsAndNotifiesTheMentionedUser() {
        when(mentionRepository.findAllByPostId(POST_ID)).thenReturn(List.of());
        when(userRepository.findAllByUsernameIn(Set.of("abhiai", "missing")))
                .thenReturn(List.of(mentioned));

        mentionService.synchronize(post, "Hello @AbhiAI and @missing");

        ArgumentCaptor<PostMention> captor = ArgumentCaptor.forClass(PostMention.class);
        verify(mentionRepository).save(captor.capture());
        assertEquals(MENTIONED_ID, captor.getValue().getMentionedUser().getId());
        verify(notificationService).notifyMention(author, mentioned, post);
    }

    @Test
    void removesObsoleteMentionsWithoutCreatingDuplicateNotifications() {
        PostMention current = new PostMention(post, mentioned);
        when(mentionRepository.findAllByPostId(POST_ID)).thenReturn(List.of(current));

        mentionService.synchronize(post, "No mentions now");

        verify(mentionRepository).delete(current);
        verify(userRepository, never()).findAllByUsernameIn(any());
        verify(notificationService, never()).notifyMention(any(), any(), any());
    }

    @Test
    void listsMentionsOnlyAfterCheckingPostVisibility() {
        when(postAccessService.findViewablePost(AUTHOR_ID, POST_ID)).thenReturn(post);
        when(mentionRepository.findAllByPostId(POST_ID))
                .thenReturn(List.of(new PostMention(post, mentioned)));

        var response = mentionService.getPostMentions(AUTHOR_ID, POST_ID);

        assertEquals("abhiai", response.getFirst().username());
        verify(postAccessService).findViewablePost(AUTHOR_ID, POST_ID);
    }

    private User user(UUID id, String username) {
        User result = new User(username, username, username + "@example.com", "hash");
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }
}
