package com.example.studybuddyapp.api.dto;

import java.util.List;

/**
 * Single study-session entry returned for the user's session history.
 */
public class SessionHistoryItem {
    // Session id
    private long id;
    // Planned duration
    private int durationMinutes;
    // Completion state
    private Boolean completed;
    // Room channel
    private String channelName;
    // Start time
    private String startedAt;
    // End time
    private String endedAt;
    // Linked tasks
    private List<TaskSummary> tasks;

    public long getId() { return id; }
    public int getDurationMinutes() { return durationMinutes; }
    public Boolean getCompleted() { return completed; }
    public String getChannelName() { return channelName; }
    public String getStartedAt() { return startedAt; }
    public String getEndedAt() { return endedAt; }
    public List<TaskSummary> getTasks() { return tasks; }

    /**
     * Summary of a task that was linked to the recorded session.
     */
    public static class TaskSummary {
        // Task id
        private long id;
        // Task title
        private String title;
        // Task note
        private String note;
        // Completion state
        private Boolean completed;

        public long getId() { return id; }
        public String getTitle() { return title; }
        public String getNote() { return note; }
        public Boolean getCompleted() { return completed; }
    }
}
