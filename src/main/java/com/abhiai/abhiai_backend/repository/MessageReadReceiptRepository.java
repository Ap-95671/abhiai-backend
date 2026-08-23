package com.abhiai.abhiai_backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.abhiai.abhiai_backend.entity.MessageReadReceipt;

public interface MessageReadReceiptRepository extends JpaRepository<MessageReadReceipt, UUID> {

    boolean existsByMessageIdAndUserId(UUID messageId, UUID userId);
}
