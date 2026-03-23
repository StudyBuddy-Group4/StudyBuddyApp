package com.example.studybuddyapp.api.dto;

public class ReportResponse {
    private Long id;
    private Long reportingUserId;
    private String reportingUsername;
    private Long reportedUserId;
    private String reportedUsername;
    private String meetingId;
    private String reason;
    private String status;
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
