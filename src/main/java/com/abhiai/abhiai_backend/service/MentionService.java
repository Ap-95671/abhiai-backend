package com.abhiai.abhiai_backend.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.dto.mention.MentionResponse;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostMention;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.InvalidPostException;
import com.abhiai.abhiai_backend.repository.PostMentionRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class MentionService {

    private static final int MAX_MENTIONS_PER_POST = 20;
    private static final Pattern MENTION_PATTERN = Pattern.compile(
            "(?<![A-Za-z0-9_])@([A-Za-z0-9_]{3,30})");

    private final PostMentionRepository mentionRepository;
    private final UserRepository userRepository;
    private final PostAccessService postAccessService;
    private final SocialNotificationService notificationService;
    private final BlockPolicyService blockPolicyService;

    public MentionService(PostMentionRepository mentionRepository, UserRepository userRepository,
            PostAccessService postAccessService, SocialNotificationService notificationService) {
        this(mentionRepository, userRepository, postAccessService, notificationService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public MentionService(
            PostMentionRepository mentionRepository,
            UserRepository userRepository,
            PostAccessService postAccessService,
            SocialNotificationService notificationService,
            BlockPolicyService blockPolicyService) {
        this.mentionRepository = mentionRepository;
        this.userRepository = userRepository;
        this.postAccessService = postAccessService;
        this.notificationService = notificationService;
        this.blockPolicyService = blockPolicyService;
    }

    @Transactional
    public void synchronize(Post post, String textContent) {
        Set<String> desiredUsernames = extract(textContent);
        List<PostMention> existing = mentionRepository.findAllByPostId(post.getId());
        Map<String, PostMention> existingByUsername = existing.stream().collect(Collectors.toMap(
                relation -> relation.getMentionedUser().getUsername().toLowerCase(Locale.ROOT),
                Function.identity()));

        existingByUsername.entrySet().stream()
                .filter(entry -> !desiredUsernames.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .forEach(mentionRepository::delete);

        Set<String> additions = desiredUsernames.stream()
                .filter(username -> !existingByUsername.containsKey(username))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (additions.isEmpty()) {
            mentionRepository.flush();
            return;
        }

        Map<String, User> usersByUsername = userRepository.findAllByUsernameIn(additions).stream()
                .collect(Collectors.toMap(
                        user -> user.getUsername().toLowerCase(Locale.ROOT),
                        Function.identity()));
        additions.stream()
                .map(usersByUsername::get)
                .filter(java.util.Objects::nonNull)
                .filter(user -> blockPolicyService == null
                        || !blockPolicyService.isBlockedEitherDirection(post.getAuthor().getId(), user.getId()))
                .forEach(mentionedUser -> {
                    mentionRepository.save(new PostMention(post, mentionedUser));
                    notificationService.notifyMention(post.getAuthor(), mentionedUser, post);
                });
        mentionRepository.flush();
    }

    @Transactional(readOnly = true)
    public List<MentionResponse> getPostMentions(UUID actingUserId, UUID postId) {
        postAccessService.findViewablePost(actingUserId, postId);
        return mentionRepository.findAllByPostId(postId).stream()
                .map(PostMention::getMentionedUser)
                .map(MentionResponse::from)
                .toList();
    }

    Set<String> extract(String textContent) {
        Set<String> usernames = new LinkedHashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(textContent == null ? "" : textContent);
        while (matcher.find()) {
            usernames.add(matcher.group(1).toLowerCase(Locale.ROOT));
            if (usernames.size() > MAX_MENTIONS_PER_POST) {
                throw new InvalidPostException(
                        "A post can mention at most " + MAX_MENTIONS_PER_POST + " users");
            }
        }
        return usernames;
    }
}
