package com.example.studybuddyapp.api.dto;

/**
 * Request body for submitting a moderation report against another user.
 */
public class ReportRequest {
    // Reporter id
    private long reportingUserId;
    // Reported user id
    private long reportedUserId;
    // Meeting id or channel
    private String meetingId;
    // Written reason
    private String reason;
    // Client timestamp
    private long timestamp;

    /**
     * Creates a report request with the reporter, target user, meeting, and reason.
     */
    public ReportRequest(long reportingUserId, long reportedUserId, String meetingId, String reason, long timestamp) {
        this.reportingUserId = reportingUserId;
        this.reportedUserId = reportedUserId;
        this.meetingId = meetingId;
        this.reason = reason;
        this.timestamp = timestamp;
    }
}
