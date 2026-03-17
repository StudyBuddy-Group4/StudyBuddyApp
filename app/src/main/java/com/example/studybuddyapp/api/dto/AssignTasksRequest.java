package com.example.studybuddyapp.api.dto;

public class AssignTasksRequest {
    private long sessionId;

    public AssignTasksRequest(long sessionId) {
        this.sessionId = sessionId;
    }

    public long getSessionId() { return sessionId; }
}
