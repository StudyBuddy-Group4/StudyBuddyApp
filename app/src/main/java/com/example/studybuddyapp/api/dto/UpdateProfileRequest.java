package com.example.studybuddyapp.api.dto;

public class UpdateProfileRequest {
    private String username;
    private String email;

    public UpdateProfileRequest(String username, String email) {
        this.username = username;
        this.email = email;
    }
}
