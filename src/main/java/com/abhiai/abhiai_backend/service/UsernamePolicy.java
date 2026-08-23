package com.abhiai.abhiai_backend.service;

import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.abhiai.abhiai_backend.config.SocialProfileProperties;
import com.abhiai.abhiai_backend.exception.InvalidUsernameException;

@Component
public class UsernamePolicy {

    private final SocialProfileProperties properties;

    public UsernamePolicy(SocialProfileProperties properties) {
        this.properties = properties;
    }

    public String normalizeAndValidate(String username) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() < properties.getUsernameMinLength()
                || normalized.length() > properties.getUsernameMaxLength()
                || !Pattern.matches(properties.getUsernamePattern(), normalized)
                || isReserved(normalized)) {
            throw new InvalidUsernameException(
                    "Username must be " + properties.getUsernameMinLength() + "-"
                            + properties.getUsernameMaxLength()
                            + " characters and use only allowed characters");
        }
        return normalized;
    }

    public String generateInitialUsername(String email, Predicate<String> usernameExists) {
        String emailPrefix = email.substring(0, email.indexOf('@'))
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]", "");
        String base = emailPrefix.length() < properties.getUsernameMinLength() ? "user" : emailPrefix;
        base = base.substring(0, Math.min(base.length(), properties.getUsernameMaxLength()));

        String candidate = base;
        int suffix = 2;
        while (isReserved(candidate) || usernameExists.test(candidate)) {
            String suffixText = "_" + suffix++;
            int baseLimit = properties.getUsernameMaxLength() - suffixText.length();
            candidate = base.substring(0, Math.min(base.length(), baseLimit)) + suffixText;
        }
        return candidate;
    }

    private boolean isReserved(String username) {
        return properties.getReservedUsernames().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(username::equals);
    }
}
