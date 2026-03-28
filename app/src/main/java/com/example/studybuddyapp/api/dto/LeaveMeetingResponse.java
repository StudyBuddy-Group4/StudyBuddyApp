package com.example.studybuddyapp.api.dto;

/**
 * Response returned after notifying the backend that a user left a meeting.
 */
public class LeaveMeetingResponse {
    // Backend result flag
    private boolean success;
    // Result message
    private String message;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}
