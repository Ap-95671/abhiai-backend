package com.abhiai.abhiai_backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.abhiai.abhiai_backend.entity.Hashtag;

public interface HashtagRepository extends JpaRepository<Hashtag, UUID> {

    Optional<Hashtag> findByNormalizedTag(String normalizedTag);

    Page<Hashtag> findByPostCountGreaterThan(long minimumPostCount, Pageable pageable);
    @Query(value="""
            select h.* from hashtags h join post_hashtags ph on ph.hashtag_id=h.id
            join posts p on p.id=ph.post_id
            where p.deleted_at is null and p.created_at >= current_timestamp - interval '7 days'
            group by h.id
            order by (count(distinct p.author_id)*5 + count(p.id)*2
              + coalesce(sum(p.like_count+p.reply_count*2+p.repost_count*3),0)) desc,
              max(p.created_at) desc
            """,nativeQuery=true)
    java.util.List<Hashtag> findTrendingRecent(Pageable pageable);

    @Query("""
            select hashtag
            from Hashtag hashtag
            where lower(hashtag.normalizedTag) like lower(concat(:query, '%'))
               or lower(hashtag.displayTag) like lower(concat(:query, '%'))
            """)
    Page<Hashtag> search(@Param("query") String query, Pageable pageable);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into hashtags (id, normalized_tag, display_tag, post_count, created_at)
            values (:id, :normalizedTag, :displayTag, 0, current_timestamp)
            on conflict (normalized_tag) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("normalizedTag") String normalizedTag,
            @Param("displayTag") String displayTag);

    @Modifying(flushAutomatically = true)
    @Query("update Hashtag hashtag set hashtag.postCount = hashtag.postCount + 1 where hashtag.id = :hashtagId")
    int incrementPostCount(@Param("hashtagId") UUID hashtagId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update Hashtag hashtag
            set hashtag.postCount = case when hashtag.postCount > 0 then hashtag.postCount - 1 else 0 end
            where hashtag.id = :hashtagId
            """)
    int decrementPostCount(@Param("hashtagId") UUID hashtagId);
}
