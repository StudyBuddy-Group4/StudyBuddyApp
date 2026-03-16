package com.example.studybuddyapp.api.dto;

public class LeaveMeetingResponse {

    private final boolean success;
    private final String message;

    public LeaveMeetingResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
