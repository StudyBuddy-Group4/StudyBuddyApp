package com.example.studybuddyapp.api.dto;

public class StartSessionRequest {
    private int durationMinutes;
    private String channelName;

    public StartSessionRequest(int durationMinutes, String channelName) {
        this.durationMinutes = durationMinutes;
        this.channelName = channelName;
    }

    public int getDurationMinutes() { return durationMinutes; }
    public String getChannelName() { return channelName; }
}
