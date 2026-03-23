package com.example.studybuddyapp.api.dto;

import java.util.Set;

public class JoinMeetingResponse {
    private String meetingId;
    private String channelName;
    private int duration;
    private Set<MeetingParticipant> participants;

    public String getMeetingId() { return meetingId; }
    public String getChannelName() { return channelName; }
    public int getDuration() { return duration; }
    public Set<MeetingParticipant> getParticipants() { return participants; }

    public static class MeetingParticipant {
        private long id;
        private String username;

        public long getId() { return id; }
        public String getUsername() { return username; }
    }
}
