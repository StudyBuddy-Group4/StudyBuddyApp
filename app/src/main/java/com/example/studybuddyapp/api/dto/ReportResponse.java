package com.example.studybuddyapp.api.dto;

/**
 * Response model for moderation reports shown in admin and history screens.
 */
public class ReportResponse {
    // Report id
    private Long id;
    // Reporter id
    private Long reportingUserId;
    // Reporter name
    private String reportingUsername;
    // Reported user id
    private Long reportedUserId;
    // Reported username
    private String reportedUsername;
    // Meeting id
    private String meetingId;
    // Report text
    private String reason;
    // Backend status
    private String status;
    // Created time
    private String timestamp;

    public Long getId() { return id; }
    public Long getReportingUserId() { return reportingUserId; }
    public String getReportingUsername() { return reportingUsername; }
    public Long getReportedUserId() { return reportedUserId; }
    public String getReportedUsername() { return reportedUsername; }
    public String getMeetingId() { return meetingId; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public String getTimestamp() { return timestamp; }
}
