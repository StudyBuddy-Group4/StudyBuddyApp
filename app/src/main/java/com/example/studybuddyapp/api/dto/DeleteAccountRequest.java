package com.example.studybuddyapp.api.dto;

/**
 * Request body used to confirm account deletion with the current password.
 */
public class DeleteAccountRequest {
    // Current password
    private String password;

    /**
     * Creates a delete-account request for the supplied password.
     */
    public DeleteAccountRequest(String password) {
        this.password = password;
    }
}
