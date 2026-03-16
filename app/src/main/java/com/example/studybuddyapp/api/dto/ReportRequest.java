package com.example.studybuddyapp.api.dto;

/**
 * Data Transfer Object (DTO) for submitting a report against a user.
 * This class encapsulates all necessary data required by the backend
 * to process a moderation flag, fulfilling requirement [MOD-3].
 * It includes the reporting user, the reported user, the meeting context,
 * and the specific written reason for the flag.
 */
public class ReportRequest {
    // The unique identifier of the user who is submitting the report.
    private long reportingUserId;
    
    // The unique identifier of the user who is being reported.
    private long reportedUserId;
    
    // The unique identifier or channel name of the meeting where the incident occurred.
    private String meetingId;
    
    // The written reason provided by the reporting user describing the inappropriate behavior.
    private String reason;
    
    // The exact timestamp when the report was generated on the client side.
    private long timestamp;

    /**
     * Constructor to initialize a new ReportRequest.
     * * @param reportingUserId ID of the user reporting.
     * @param reportedUserId ID of the user being reported.
     * @param meetingId The channel or meeting identifier.
     * @param reason The written explanation of the offense.
     * @param timestamp The time the offense was reported.
     */
    public ReportRequest(long reportingUserId, long reportedUserId, String meetingId, String reason, long timestamp) {
        this.reportingUserId = reportingUserId;
        this.reportedUserId = reportedUserId;
        this.meetingId = meetingId;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    // Getters and Setters can be generated here...
}