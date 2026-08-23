package com.abhiai.abhiai_backend.service;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.hashtag.HashtagResponse;
import com.abhiai.abhiai_backend.dto.post.PostResponse;
import com.abhiai.abhiai_backend.entity.Hashtag;
import com.abhiai.abhiai_backend.entity.Post;
import com.abhiai.abhiai_backend.entity.PostHashtag;
import com.abhiai.abhiai_backend.exception.HashtagNotFoundException;
import com.abhiai.abhiai_backend.exception.InvalidHashtagException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.HashtagRepository;
import com.abhiai.abhiai_backend.repository.PostHashtagRepository;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class HashtagService {

    private static final int MAX_TAGS_PER_POST = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final Pattern HASHTAG_PATTERN = Pattern.compile(
            "(?<![\\p{L}\\p{N}_])#([\\p{L}\\p{N}_]{1,50})",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern TAG_VALUE_PATTERN = Pattern.compile(
            "[\\p{L}\\p{N}_]{1,50}", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Sort POST_SORT = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    private static final Sort TRENDING_SORT = Sort.by(
            Sort.Order.desc("postCount"), Sort.Order.asc("normalizedTag"), Sort.Order.asc("id"));

    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository relationRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public HashtagService(
            HashtagRepository hashtagRepository,
            PostHashtagRepository relationRepository,
            PostRepository postRepository,
            UserRepository userRepository) {
        this.hashtagRepository = hashtagRepository;
        this.relationRepository = relationRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void synchronize(Post post, String textContent) {
        Map<String, String> desired = extract(textContent);
        List<PostHashtag> currentRelations = relationRepository.findAllByPostId(post.getId());
        Map<String, PostHashtag> current = new LinkedHashMap<>();
        currentRelations.forEach(relation -> current.put(
                relation.getHashtag().getNormalizedTag(), relation));

        current.entrySet().stream()
                .filter(entry -> !desired.containsKey(entry.getKey()))
                .forEach(entry -> {
                    relationRepository.delete(entry.getValue());
                    hashtagRepository.decrementPostCount(entry.getValue().getHashtag().getId());
                });

        desired.entrySet().stream()
                .filter(entry -> !current.containsKey(entry.getKey()))
                .forEach(entry -> {
                    Hashtag hashtag = ensureHashtag(entry.getKey(), entry.getValue());
                    relationRepository.save(new PostHashtag(post, hashtag));
                    hashtagRepository.incrementPostCount(hashtag.getId());
                });
        relationRepository.flush();
    }

    @Transactional(readOnly = true)
    public PageResponse<HashtagResponse> trending(Pageable pageable) {
        Page<Hashtag> hashtags = hashtagRepository.findByPostCountGreaterThan(
                0, normalize(pageable, TRENDING_SORT));
        return PageResponse.from(hashtags, HashtagResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> posts(UUID actingUserId, String requestedTag, Pageable pageable) {
        if (!userRepository.existsById(actingUserId)) throw new UserNotFoundException();
        String normalized = normalizeTag(requestedTag);
        Hashtag hashtag = hashtagRepository.findByNormalizedTag(normalized)
                .orElseThrow(HashtagNotFoundException::new);
        Page<Post> posts = postRepository.findVisiblePostsByHashtag(
                actingUserId, hashtag.getId(), normalize(pageable, POST_SORT));
        return PageResponse.from(posts, PostResponse::from);
    }

    Map<String, String> extract(String textContent) {
        Map<String, String> tags = new LinkedHashMap<>();
        Matcher matcher = HASHTAG_PATTERN.matcher(textContent == null ? "" : textContent);
        while (matcher.find()) {
            String display = Normalizer.normalize(matcher.group(1), Normalizer.Form.NFKC);
            String normalized = display.toLowerCase(Locale.ROOT);
            if (normalized.codePoints().noneMatch(Character::isLetterOrDigit)) continue;
            tags.putIfAbsent(normalized, display);
            if (tags.size() > MAX_TAGS_PER_POST) {
                throw new InvalidHashtagException("A post can contain at most " + MAX_TAGS_PER_POST + " hashtags");
            }
        }
        return tags;
    }

    private Hashtag ensureHashtag(String normalized, String display) {
        hashtagRepository.insertIfAbsent(UUID.randomUUID(), normalized, display);
        return hashtagRepository.findByNormalizedTag(normalized)
                .orElseThrow(() -> new IllegalStateException("Hashtag could not be created"));
    }

    private String normalizeTag(String requestedTag) {
        String value = requestedTag == null ? "" : requestedTag.trim();
        if (value.startsWith("#")) value = value.substring(1);
        value = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        if (!TAG_VALUE_PATTERN.matcher(value).matches()
                || value.codePoints().noneMatch(Character::isLetterOrDigit)) {
            throw new InvalidHashtagException("Hashtag must contain 1 to 50 letters, numbers, or underscores");
        }
        return value;
    }

    private Pageable normalize(Pageable pageable, Sort sort) {
        int size = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }
}
