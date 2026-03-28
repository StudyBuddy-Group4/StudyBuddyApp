package com.example.studybuddyapp.api.dto;

/**
 * Request body used when creating a new task.
 */
public class CreateTaskRequest {
    // Task title
    private String title;
    // Optional note
    private String note;

    /**
     * Creates a task request with a required title and optional note.
     */
    public CreateTaskRequest(String title, String note) {
        this.title = title;
        this.note = note;
    }

    public String getTitle() { return title; }
    public String getNote() { return note; }
}
