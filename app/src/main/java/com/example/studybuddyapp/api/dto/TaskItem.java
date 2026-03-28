package com.example.studybuddyapp.api.dto;

/**
 * Task model used by the task list and session-history screens.
 */
public class TaskItem {
    // Task id
    private long id;
    // Linked session id
    private Long sessionId;
    // Task title
    private String title;
    // Optional note
    private String note;
    // Completion state
    private Boolean completed;
    // Created time
    private String createdAt;

    public long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public String getTitle() { return title; }
    public String getNote() { return note; }
    public Boolean getCompleted() { return completed; }
    public String getCreatedAt() { return createdAt; }

    /**
     * Updates the locally cached completion state after the backend confirms a change.
     */
    public void setCompleted(Boolean completed) { this.completed = completed; }
}
