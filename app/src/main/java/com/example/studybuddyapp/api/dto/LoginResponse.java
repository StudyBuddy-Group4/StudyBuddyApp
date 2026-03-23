package com.example.studybuddyapp.api.dto;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    private String token;
    private Long userId;
    private String username;


    @SerializedName("admin")
    private boolean isAdmin;

    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public boolean isAdmin() { return isAdmin; }
}