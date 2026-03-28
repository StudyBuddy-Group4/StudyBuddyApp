package com.example.studybuddyapp.api;

import com.example.studybuddyapp.api.dto.JoinMeetingResponse;
import com.example.studybuddyapp.api.dto.LeaveMeetingResponse;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Matching and room-assignment endpoints.
 */
public interface MatchingApi {

    // Join a room for the selected duration
    @POST("api/matching/join/{duration}")
    Call<JoinMeetingResponse> joinMeeting(@Path("duration") int duration);

    // Leave the current room
    @POST("api/matching/leave/{channelName}")
    Call<LeaveMeetingResponse> leaveMeeting(@Path("channelName") String channelName);
}
