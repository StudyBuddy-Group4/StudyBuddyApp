package com.example.studybuddyapp.api.dto;

import java.util.List;

public class SessionHistoryItem {
    private long id;
    private int durationMinutes;
    private Boolean completed;
    private String channelName;
    private String startedAt;
    private String endedAt;
    private List<TaskSummary> tasks;

    public long getId() { return id; }
    public int getDurationMinutes() { return durationMinutes; }
    public Boolean getCompleted() { return completed; }
    public String getChannelName() { return channelName; }
    public String getStartedAt() { return startedAt; }
    public String getEndedAt() { return endedAt; }
    public List<TaskSummary> getTasks() { return tasks; }

    public static class TaskSummary {
        private long id;
        private String title;
        private String note;
        private Boolean completed;

        public long getId() { return id; }
        public String getTitle() { return title; }
        public String getNote() { return note; }
        public Boolean getCompleted() { return completed; }
    }
}
