package com.example.studybuddyapp.api.dto;

import java.util.Set;

/**
 * Response returned when the backend places the user into a meeting room.
 */
public class JoinMeetingResponse {
    // Backend meeting id
    private String meetingId;
    // Agora channel
    private String channelName;
    // Focus duration
    private int duration;
    // Current participants
    private Set<MeetingParticipant> participants;

    public String getMeetingId() { return meetingId; }
    public String getChannelName() { return channelName; }
    public int getDuration() { return duration; }
    public Set<MeetingParticipant> getParticipants() { return participants; }

    /**
     * Minimal participant snapshot included with a join-meeting response.
     */
    public static class MeetingParticipant {
        // User id
        private long id;
        // Display name
        private String username;

        public long getId() { return id; }
        public String getUsername() { return username; }
    }
}
