package com.example.studybuddyapp.api.dto;

public class TaskItem {
    private long id;
    private Long sessionId;
    private String title;
    private String note;
    private Boolean completed;
    private String createdAt;

    public long getId() { return id; }
    public Long getSessionId() { return sessionId; }
    public String getTitle() { return title; }
    public String getNote() { return note; }
    public Boolean getCompleted() { return completed; }
    public String getCreatedAt() { return createdAt; }

    public void setCompleted(Boolean completed) { this.completed = completed; }
}
