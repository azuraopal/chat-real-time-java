package com.example.chat_real_time_java.dto;

public class JwtResponse {
    private String token;
    private Long id;
    private String username;
    private String displayName;
    private String avatarColor;

    public JwtResponse(String token, Long id, String username, String displayName, String avatarColor) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.avatarColor = avatarColor;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarColor() {
        return avatarColor;
    }

    public void setAvatarColor(String avatarColor) {
        this.avatarColor = avatarColor;
    }
}
