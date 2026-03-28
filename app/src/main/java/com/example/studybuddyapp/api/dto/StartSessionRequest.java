package com.example.studybuddyapp.api.dto;

/**
 * Request body for creating a backend focus-session record.
 */
public class StartSessionRequest {
    // Focus duration
    private int durationMinutes;
    // Room channel
    private String channelName;

    /**
     * Creates a session-start request for the selected duration and channel.
     */
    public StartSessionRequest(int durationMinutes, String channelName) {
        this.durationMinutes = durationMinutes;
        this.channelName = channelName;
    }

    public int getDurationMinutes() { return durationMinutes; }
    public String getChannelName() { return channelName; }
}
