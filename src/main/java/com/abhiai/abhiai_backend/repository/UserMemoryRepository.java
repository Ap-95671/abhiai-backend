package com.abhiai.abhiai_backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.abhiai.abhiai_backend.entity.UserMemory;

public interface UserMemoryRepository extends JpaRepository<UserMemory, UUID> {
    List<UserMemory> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);
    Optional<UserMemory> findByIdAndUserId(UUID id, UUID userId);
    long countByUserId(UUID userId);
    void deleteAllByUserId(UUID userId);
}
