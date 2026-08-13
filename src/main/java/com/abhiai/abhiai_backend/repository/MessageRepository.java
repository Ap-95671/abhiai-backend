package com.abhiai.abhiai_backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.abhiai.abhiai_backend.entity.Message;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findAllByConversationIdOrderByCreatedAtAscIdAsc(UUID conversationId);
}
