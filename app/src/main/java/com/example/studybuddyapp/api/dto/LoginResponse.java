package com.example.studybuddyapp.api.dto;

public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private boolean isAdmin;

    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public boolean isAdmin() { return isAdmin; }
}
