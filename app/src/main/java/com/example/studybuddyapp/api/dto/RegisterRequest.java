package com.example.studybuddyapp.api.dto;

/**
 * Request body for registering a new account.
 */
public class RegisterRequest {
    // Username
    private String username;
    // Email address
    private String email;
    // Raw password
    private String password;

    /**
     * Creates a register request from the sign-up form values.
     */
    public RegisterRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
}
