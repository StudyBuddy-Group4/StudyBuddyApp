package com.example.studybuddyapp.api.dto;

/**
 * Response returned after the backend creates a focus-session record.
 */
public class StartSessionResponse {
    // Created session id
    private long sessionId;

    public long getSessionId() { return sessionId; }
}
