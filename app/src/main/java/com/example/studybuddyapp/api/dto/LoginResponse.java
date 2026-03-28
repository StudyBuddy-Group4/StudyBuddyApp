package com.example.studybuddyapp.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Response returned after a successful login.
 */
public class LoginResponse {
    // JWT token
    private String token;
    // Backend user id
    private Long userId;
    // Username
    private String username;

    @SerializedName("admin")
    // Admin flag
    private boolean isAdmin;

    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public boolean isAdmin() { return isAdmin; }
}
