package com.example.studybuddyapp.api.dto;

import com.google.gson.annotations.SerializedName;

public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    
    @SerializedName("admin")
    private boolean isAdmin;
    
    @SerializedName("banned")
    private boolean isBanned;
    
    private double rating;

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public boolean isAdmin() { return isAdmin; }
    public boolean isBanned() { return isBanned; }
    public double getRating() { return rating; }
}