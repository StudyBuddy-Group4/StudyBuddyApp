package com.example.studybuddyapp.api.dto;

public class ReportResponse {
    private Long id;
    private Long reportingUserId;
    private Long reportedUserId;
    private String meetingId;
    private String reason;
    private String status;

    // Getters so Retrofit can read the JSON data from your backend
    public Long getId() { return id; }
    public Long getReportingUserId() { return reportingUserId; }
    public Long getReportedUserId() { return reportedUserId; }
    public String getMeetingId() { return meetingId; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
}