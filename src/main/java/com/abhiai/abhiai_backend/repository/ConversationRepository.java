package com.abhiai.abhiai_backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.abhiai.abhiai_backend.entity.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);

    @Query(
            value = """
                    select distinct c from Conversation c
                    where c.user.id = :userId
                      and (
                        lower(c.title) like lower(concat('%', :query, '%'))
                        or exists (
                          select m.id from Message m
                          where m.conversation = c
                            and lower(m.content) like lower(concat('%', :query, '%'))
                        )
                      )
                    order by c.updatedAt desc, c.id asc
                    """,
            countQuery = """
                    select count(distinct c) from Conversation c
                    where c.user.id = :userId
                      and (
                        lower(c.title) like lower(concat('%', :query, '%'))
                        or exists (
                          select m.id from Message m
                          where m.conversation = c
                            and lower(m.content) like lower(concat('%', :query, '%'))
                        )
                      )
                    """)
    Page<Conversation> searchOwnedConversations(
            @Param("userId") UUID userId,
            @Param("query") String query,
            Pageable pageable);
}
