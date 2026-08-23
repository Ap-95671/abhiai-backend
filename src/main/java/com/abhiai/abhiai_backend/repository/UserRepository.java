package com.abhiai.abhiai_backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.abhiai.abhiai_backend.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("""
            select user
            from User user
            where user.id <> :actingUserId
              and not exists (
                select follow.id
                from Follow follow
                where follow.follower.id = :actingUserId
                  and follow.following.id = user.id
              )
            order by user.followerCount desc,
                     user.postCount desc,
                     user.createdAt desc,
                     user.id asc
            """)
    List<User> findSuggestedAccounts(
            @Param("actingUserId") UUID actingUserId,
            Pageable pageable);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, UUID id);

    List<User> findAllByUsernameIn(Collection<String> usernames);

    @Query("select count(user) > 0 from User user where user.profileMedia.id = :mediaId or user.coverMedia.id = :mediaId")
    boolean isUsedAsProfileMedia(@Param("mediaId") UUID mediaId);

    @Query("""
            select user
            from User user
            where lower(user.username) like lower(concat(:query, '%'))
               or lower(user.displayName) like lower(concat(:query, '%'))
            """)
    Page<User> searchByUsernameOrDisplayName(
            @Param("query") String query,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update User user
            set user.followingCount = user.followingCount + 1
            where user.id = :userId
            """)
    int incrementFollowingCount(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update User user
            set user.followerCount = user.followerCount + 1
            where user.id = :userId
            """)
    int incrementFollowerCount(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update User user
            set user.followingCount =
                case when user.followingCount > 0 then user.followingCount - 1 else 0 end
            where user.id = :userId
            """)
    int decrementFollowingCount(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update User user
            set user.followerCount =
                case when user.followerCount > 0 then user.followerCount - 1 else 0 end
            where user.id = :userId
            """)
    int decrementFollowerCount(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update User user
            set user.postCount = user.postCount + 1
            where user.id = :userId
            """)
    int incrementPostCount(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update User user
            set user.postCount =
                case when user.postCount > 0 then user.postCount - 1 else 0 end
            where user.id = :userId
            """)
    int decrementPostCount(@Param("userId") UUID userId);
}
