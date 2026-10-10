package com.ledgertracker.dto;

import java.time.LocalDateTime;

public class UserDto {
    private Long id;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String authProvider;
    private boolean emailVerified;
    private boolean hasPassword;
    private boolean hasGoogleLinked;
    private LocalDateTime createdAt;

    public UserDto() {
    }

    public UserDto(Long id, String email, String displayName, String avatarUrl, String authProvider,
                   boolean emailVerified, boolean hasPassword, boolean hasGoogleLinked, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.authProvider = authProvider;
        this.emailVerified = emailVerified;
        this.hasPassword = hasPassword;
        this.hasGoogleLinked = hasGoogleLinked;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isHasPassword() {
        return hasPassword;
    }

    public void setHasPassword(boolean hasPassword) {
        this.hasPassword = hasPassword;
    }

    public boolean isHasGoogleLinked() {
        return hasGoogleLinked;
    }

    public void setHasGoogleLinked(boolean hasGoogleLinked) {
        this.hasGoogleLinked = hasGoogleLinked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
