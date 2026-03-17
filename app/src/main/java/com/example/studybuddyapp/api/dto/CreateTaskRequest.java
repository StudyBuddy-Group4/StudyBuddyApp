package com.example.studybuddyapp.api.dto;

public class CreateTaskRequest {
    private String title;
    private String note;

    public CreateTaskRequest(String title, String note) {
        this.title = title;
        this.note = note;
    }

    public String getTitle() { return title; }
    public String getNote() { return note; }
}
