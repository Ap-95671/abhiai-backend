package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.abhiai.abhiai_backend.config.SocialProfileProperties;
import com.abhiai.abhiai_backend.exception.InvalidUsernameException;

class UsernamePolicyTest {

    private UsernamePolicy usernamePolicy;

    @BeforeEach
    void setUp() {
        SocialProfileProperties properties = new SocialProfileProperties();
        properties.setReservedUsernames(Set.of("admin", "me"));
        usernamePolicy = new UsernamePolicy(properties);
    }

    @Test
    void normalizesAValidUsername() {
        assertEquals("abhishek_01", usernamePolicy.normalizeAndValidate("  Abhishek_01 "));
    }

    @Test
    void rejectsInvalidAndReservedUsernames() {
        assertThrows(InvalidUsernameException.class, () -> usernamePolicy.normalizeAndValidate("not-valid!"));
        assertThrows(InvalidUsernameException.class, () -> usernamePolicy.normalizeAndValidate("ADMIN"));
    }

    @Test
    void createsAnAvailableInitialUsername() {
        String username = usernamePolicy.generateInitialUsername(
                "Abhi.Shek@example.com",
                candidate -> candidate.equals("abhishek"));

        assertEquals("abhishek_2", username);
    }
}
