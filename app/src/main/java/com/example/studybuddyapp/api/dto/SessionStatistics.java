package com.example.studybuddyapp.api.dto;

public class SessionStatistics {
    private double rating;
    private long totalFocusTimeMinutes;
    private long completedCount;
    private long totalCount;

    public double getRating() { return rating; }
    public long getTotalFocusTimeMinutes() { return totalFocusTimeMinutes; }
    public long getCompletedCount() { return completedCount; }
    public long getTotalCount() { return totalCount; }
}
