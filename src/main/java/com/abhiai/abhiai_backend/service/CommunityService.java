package com.abhiai.abhiai_backend.service;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.dto.common.PageResponse;
import com.abhiai.abhiai_backend.dto.community.CommunityResponse;
import com.abhiai.abhiai_backend.dto.community.CreateCommunityPostRequest;
import com.abhiai.abhiai_backend.dto.community.CreateCommunityRequest;
import com.abhiai.abhiai_backend.dto.post.PostResponse;
import com.abhiai.abhiai_backend.entity.Community;
import com.abhiai.abhiai_backend.entity.CommunityMembership;
import com.abhiai.abhiai_backend.entity.CommunityPrivacy;
import com.abhiai.abhiai_backend.entity.CommunityRole;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.exception.CommunityMembershipNotFoundException;
import com.abhiai.abhiai_backend.exception.CommunityNotFoundException;
import com.abhiai.abhiai_backend.exception.CommunitySlugAlreadyExistsException;
import com.abhiai.abhiai_backend.exception.DuplicateCommunityMembershipException;
import com.abhiai.abhiai_backend.exception.InvalidCommunityException;
import com.abhiai.abhiai_backend.exception.UnauthorizedActionException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.CommunityMembershipRepository;
import com.abhiai.abhiai_backend.repository.CommunityRepository;
import com.abhiai.abhiai_backend.repository.PostRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class CommunityService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> RESERVED_SLUGS = Set.of(
            "admin", "api", "create", "discover", "joined", "me", "mine", "new", "search", "settings");

    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostService postService;

    public CommunityService(
            CommunityRepository communityRepository,
            CommunityMembershipRepository membershipRepository,
            PostRepository postRepository,
            UserRepository userRepository,
            PostService postService) {
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.postService = postService;
    }

    @Transactional
    public CommunityResponse createCommunity(UUID actingUserId, CreateCommunityRequest request) {
        User owner = requireUser(actingUserId);
        CommunityPrivacy privacy = request.privacy() == null ? CommunityPrivacy.PUBLIC : request.privacy();
        if (privacy != CommunityPrivacy.PUBLIC) {
            throw new InvalidCommunityException(
                    "Private communities and membership approvals are planned for a later phase");
        }
        String name = normalizeRequired(request.name(), "Community name is required");
        String slug = normalizeSlug(request.slug(), name);
        if (communityRepository.existsBySlugIgnoreCase(slug)) {
            throw new CommunitySlugAlreadyExistsException();
        }
        Community community = communityRepository.saveAndFlush(new Community(
                name,
                slug,
                normalizeRequired(request.description(), "Community description is required"),
                normalizeUrl(request.iconUrl(), "Community icon"),
                normalizeUrl(request.bannerUrl(), "Community banner"),
                owner,
                privacy));
        membershipRepository.saveAndFlush(
                new CommunityMembership(community, owner, CommunityRole.OWNER));
        return CommunityResponse.from(community, CommunityRole.OWNER);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommunityResponse> getCommunities(UUID actingUserId, Pageable pageable) {
        requireUser(actingUserId);
        Page<Community> communities = communityRepository
                .findAllByPrivacyOrderByMemberCountDescCreatedAtDescIdDesc(
                        CommunityPrivacy.PUBLIC, normalize(pageable));
        List<UUID> ids = communities.getContent().stream().map(Community::getId).toList();
        Map<UUID, CommunityMembership> memberships = membershipRepository
                .findAllByUserIdAndCommunityIdIn(actingUserId, ids).stream()
                .collect(Collectors.toMap(
                        membership -> membership.getCommunity().getId(), Function.identity()));
        return PageResponse.from(communities, community -> {
            CommunityMembership membership = memberships.get(community.getId());
            return CommunityResponse.from(
                    community, membership == null ? null : membership.getRole());
        });
    }

    @Transactional(readOnly = true)
    public CommunityResponse getCommunity(UUID actingUserId, String requestedSlug) {
        Community community = requirePublicCommunity(requestedSlug);
        return CommunityResponse.from(community, roleFor(community.getId(), actingUserId));
    }

    @Transactional
    public CommunityResponse joinCommunity(UUID actingUserId, String requestedSlug) {
        User user = requireUser(actingUserId);
        Community community = requirePublicCommunityForUpdate(requestedSlug);
        if (membershipRepository.existsByCommunityIdAndUserId(community.getId(), actingUserId)) {
            throw new DuplicateCommunityMembershipException();
        }
        membershipRepository.saveAndFlush(
                new CommunityMembership(community, user, CommunityRole.MEMBER));
        community.addMember();
        communityRepository.flush();
        return CommunityResponse.from(community, CommunityRole.MEMBER);
    }

    @Transactional
    public CommunityResponse leaveCommunity(UUID actingUserId, String requestedSlug) {
        Community community = requirePublicCommunityForUpdate(requestedSlug);
        CommunityMembership membership = membershipRepository
                .findByCommunityIdAndUserId(community.getId(), actingUserId)
                .orElseThrow(CommunityMembershipNotFoundException::new);
        if (membership.getRole() == CommunityRole.OWNER) {
            throw new InvalidCommunityException(
                    "The community owner cannot leave until ownership transfer is available");
        }
        membershipRepository.delete(membership);
        membershipRepository.flush();
        community.removeMember();
        communityRepository.flush();
        return CommunityResponse.from(community, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getCommunityFeed(
            UUID actingUserId,
            String requestedSlug,
            Pageable pageable) {
        requireUser(actingUserId);
        Community community = requirePublicCommunity(requestedSlug);
        return PageResponse.from(
                postRepository.findCommunityFeed(community.getId(), actingUserId, normalize(pageable)),
                PostResponse::from);
    }

    @Transactional
    public PostResponse publishPost(
            UUID actingUserId,
            String requestedSlug,
            CreateCommunityPostRequest request) {
        Community community = requirePublicCommunity(requestedSlug);
        if (!membershipRepository.existsByCommunityIdAndUserId(community.getId(), actingUserId)) {
            throw new UnauthorizedActionException("Join this community before publishing a post");
        }
        return postService.createCommunityPost(
                actingUserId, community, request.textContent(), request.mediaIds());
    }

    private Community requirePublicCommunity(String requestedSlug) {
        Community community = communityRepository.findBySlugIgnoreCase(normalizeLookupSlug(requestedSlug))
                .orElseThrow(CommunityNotFoundException::new);
        if (community.getPrivacy() != CommunityPrivacy.PUBLIC) throw new CommunityNotFoundException();
        return community;
    }

    private Community requirePublicCommunityForUpdate(String requestedSlug) {
        Community community = communityRepository.findBySlugForUpdate(normalizeLookupSlug(requestedSlug))
                .orElseThrow(CommunityNotFoundException::new);
        if (community.getPrivacy() != CommunityPrivacy.PUBLIC) throw new CommunityNotFoundException();
        return community;
    }

    private CommunityRole roleFor(UUID communityId, UUID userId) {
        return membershipRepository.findByCommunityIdAndUserId(communityId, userId)
                .map(CommunityMembership::getRole)
                .orElse(null);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    private String normalizeRequired(String value, String message) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) throw new InvalidCommunityException(message);
        return normalized;
    }

    private String normalizeSlug(String requestedSlug, String name) {
        String source = requestedSlug == null || requestedSlug.isBlank() ? name : requestedSlug;
        String normalized = source.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (normalized.isEmpty() || normalized.length() > 64
                || !normalized.matches("[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")) {
            throw new InvalidCommunityException(
                    "Community slug must contain lowercase letters, numbers, and hyphen separators");
        }
        if (RESERVED_SLUGS.contains(normalized)) {
            throw new InvalidCommunityException("This community slug is reserved");
        }
        return normalized;
    }

    private String normalizeLookupSlug(String slug) {
        if (slug == null || slug.isBlank()) throw new CommunityNotFoundException();
        return slug.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUrl(String value, String fieldName) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) return null;
        try {
            URI uri = URI.create(normalized);
            if (!("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new InvalidCommunityException(fieldName + " must be a valid HTTP or HTTPS URL");
        }
        return normalized;
    }

    private Pageable normalize(Pageable pageable) {
        int page = Math.max(0, pageable.getPageNumber());
        int size = Math.max(1, Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
        return PageRequest.of(page, size);
    }
}
