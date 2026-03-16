package com.example.studybuddyapp.api.dto;

import java.util.Set;

public class JoinMeetingResponse {

    private final String meetingId;
    private final String channelName;
    private final int duration;
    private final Set<Participant> participants;

    public JoinMeetingResponse(String meetingId, String channelName, int duration, Set<Participant> participants) {
        this.meetingId = meetingId;
        this.channelName = channelName;
        this.duration = duration;
        this.participants = participants;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public String getChannelName() {
        return channelName;
    }

    public int getDuration() {
        return duration;
    }

    public Set<Participant> getParticipants() {
        return participants;
    }
}
