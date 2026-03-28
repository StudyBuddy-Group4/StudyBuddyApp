package com.example.studybuddyapp.api.dto;

/**
 * Aggregate statistics used by the statistics screen.
 */
public class SessionStatistics {
    // Average rating
    private double rating;
    // Total focus time
    private long totalFocusTimeMinutes;
    // Completed sessions
    private long completedCount;
    // All sessions
    private long totalCount;

    public double getRating() { return rating; }
    public long getTotalFocusTimeMinutes() { return totalFocusTimeMinutes; }
    public long getCompletedCount() { return completedCount; }
    public long getTotalCount() { return totalCount; }
}
