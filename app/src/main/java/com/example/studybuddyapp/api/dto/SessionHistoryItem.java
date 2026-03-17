package com.example.studybuddyapp.api.dto;

public class SessionHistoryItem {
    private long id;
    private int durationMinutes;
    private Boolean completed;
    private String channelName;
    private String startedAt;
    private String endedAt;

    public long getId() { return id; }
    public int getDurationMinutes() { return durationMinutes; }
    public Boolean getCompleted() { return completed; }
    public String getChannelName() { return channelName; }
    public String getStartedAt() { return startedAt; }
    public String getEndedAt() { return endedAt; }
}
