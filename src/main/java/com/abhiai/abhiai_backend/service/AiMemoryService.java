package com.abhiai.abhiai_backend.service;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.abhiai.abhiai_backend.dto.memory.MemorySettingsResponse;
import com.abhiai.abhiai_backend.dto.memory.UserMemoryResponse;
import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.entity.UserMemory;
import com.abhiai.abhiai_backend.exception.InvalidMemoryException;
import com.abhiai.abhiai_backend.exception.UserNotFoundException;
import com.abhiai.abhiai_backend.repository.UserMemoryRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@Service
public class AiMemoryService {

    private static final int MAX_MEMORIES = 50;
    private final UserRepository users;
    private final UserMemoryRepository memories;

    public AiMemoryService(UserRepository users, UserMemoryRepository memories) {
        this.users = users;
        this.memories = memories;
    }

    @Transactional(readOnly = true)
    public MemorySettingsResponse settings(UUID userId) {
        User user = user(userId);
        return response(user);
    }

    @Transactional
    public MemorySettingsResponse updateEnabled(UUID userId, boolean enabled) {
        User user = user(userId);
        user.changeAiMemoryEnabled(enabled);
        users.saveAndFlush(user);
        return response(user);
    }

    @Transactional
    public UserMemoryResponse create(UUID userId, String requestedContent) {
        User user = user(userId);
        if (memories.countByUserId(userId) >= MAX_MEMORIES) {
            throw new InvalidMemoryException("You can save up to " + MAX_MEMORIES + " memories. Delete one before adding another.");
        }
        String content = normalize(requestedContent);
        return UserMemoryResponse.from(memories.saveAndFlush(new UserMemory(user, content)));
    }

    @Transactional
    public void delete(UUID userId, UUID memoryId) {
        UserMemory memory = memories.findByIdAndUserId(memoryId, userId)
                .orElseThrow(() -> new InvalidMemoryException("Memory was not found"));
        memories.delete(memory);
    }

    @Transactional
    public void clear(UUID userId) {
        user(userId);
        memories.deleteAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public String augmentPrompt(UUID userId, String prompt) {
        User user = user(userId);
        if (!user.isAiMemoryEnabled()) return prompt;
        List<UserMemory> saved = memories.findAllByUserIdOrderByUpdatedAtDesc(userId);
        if (saved.isEmpty()) return prompt;
        StringBuilder context = new StringBuilder("[User-approved long-term memory]\n");
        saved.stream().limit(MAX_MEMORIES).forEach(memory -> context.append("- ").append(memory.getContent()).append('\n'));
        return prompt + "\n\n" + context
                + "Use these saved details only when relevant. Never claim they are newly inferred, and do not expose them unnecessarily.";
    }

    private MemorySettingsResponse response(User user) {
        return new MemorySettingsResponse(
                user.isAiMemoryEnabled(),
                memories.findAllByUserIdOrderByUpdatedAtDesc(user.getId()).stream().map(UserMemoryResponse::from).toList());
    }

    private User user(UUID userId) {
        return users.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value.trim(), Normalizer.Form.NFKC)
                .replaceAll("\\s+", " ");
        if (normalized.isBlank() || normalized.length() > 500) {
            throw new InvalidMemoryException("Memory must contain between 1 and 500 characters");
        }
        return normalized;
    }
}
