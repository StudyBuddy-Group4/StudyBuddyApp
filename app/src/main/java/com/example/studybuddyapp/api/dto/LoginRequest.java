package com.example.studybuddyapp.api.dto;

/**
 * Request body for logging in with either a username or an email address.
 */
public class LoginRequest {
    // Username or email
    private String usernameOrEmail;
    // Raw password
    private String password;

    /**
     * Creates a login request from the submitted credentials.
     */
    public LoginRequest(String usernameOrEmail, String password) {
        this.usernameOrEmail = usernameOrEmail;
        this.password = password;
    }
}
