package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.abhiai.abhiai_backend.entity.User;
import com.abhiai.abhiai_backend.entity.UserMemory;
import com.abhiai.abhiai_backend.repository.UserMemoryRepository;
import com.abhiai.abhiai_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AiMemoryServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock UserRepository users;
    @Mock UserMemoryRepository memories;
    private AiMemoryService service;

    @BeforeEach
    void setUp() { service = new AiMemoryService(users, memories); }

    @Test
    void memoryIsDisabledByDefaultAndDoesNotReadSavedItems() {
        User user = new User("abhishek", "Abhishek", "user@example.com", "hash");
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));

        assertEquals("Original prompt", service.augmentPrompt(USER_ID, "Original prompt"));
        assertFalse(user.isAiMemoryEnabled());
        verifyNoInteractions(memories);
    }

    @Test
    void enabledMemoryAddsOnlyUserManagedItemsToThePrompt() {
        User user = new User("abhishek", "Abhishek", "user@example.com", "hash");
        user.changeAiMemoryEnabled(true);
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(memories.findAllByUserIdOrderByUpdatedAtDesc(USER_ID))
                .thenReturn(List.of(new UserMemory(user, "Prefer concise answers.")));

        String augmented = service.augmentPrompt(USER_ID, "Explain caching");

        assertTrue(augmented.contains("Explain caching"));
        assertTrue(augmented.contains("Prefer concise answers."));
        assertTrue(augmented.contains("User-approved long-term memory"));
    }
}
