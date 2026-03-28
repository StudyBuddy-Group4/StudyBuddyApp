package com.example.studybuddyapp.api.dto;

/**
 * Request body for linking pending tasks to a backend session.
 */
public class AssignTasksRequest {
    // Target backend session
    private long sessionId;

    /**
     * Creates a request for the given backend session id.
     */
    public AssignTasksRequest(long sessionId) {
        this.sessionId = sessionId;
    }

    public long getSessionId() { return sessionId; }
}
