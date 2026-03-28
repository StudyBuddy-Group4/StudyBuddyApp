package com.example.studybuddyapp.api.dto;

/**
 * Request body for changing the stored username and email.
 */
public class UpdateProfileRequest {
    // New username
    private String username;
    // New email
    private String email;

    /**
     * Creates a profile-update request from the edited form values.
     */
    public UpdateProfileRequest(String username, String email) {
        this.username = username;
        this.email = email;
    }
}
