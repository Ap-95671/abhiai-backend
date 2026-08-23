package com.abhiai.abhiai_backend.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.social.profile")
public class SocialProfileProperties {

    private int usernameMinLength = 3;
    private int usernameMaxLength = 30;
    private String usernamePattern = "^[a-z0-9_]+$";
    private Set<String> reservedUsernames = new HashSet<>();

    public int getUsernameMinLength() {
        return usernameMinLength;
    }

    public void setUsernameMinLength(int usernameMinLength) {
        this.usernameMinLength = usernameMinLength;
    }

    public int getUsernameMaxLength() {
        return usernameMaxLength;
    }

    public void setUsernameMaxLength(int usernameMaxLength) {
        this.usernameMaxLength = usernameMaxLength;
    }

    public String getUsernamePattern() {
        return usernamePattern;
    }

    public void setUsernamePattern(String usernamePattern) {
        this.usernamePattern = usernamePattern;
    }

    public Set<String> getReservedUsernames() {
        return reservedUsernames;
    }

    public void setReservedUsernames(Set<String> reservedUsernames) {
        this.reservedUsernames = reservedUsernames;
    }
}
