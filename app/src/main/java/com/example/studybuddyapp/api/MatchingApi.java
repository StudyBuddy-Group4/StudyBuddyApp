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

    /**
     * Join a room for the selected duration
     * @param duration the duration to search a match for will be +- 5
     * @return returns a meeting response
     */
    @POST("api/matching/join/{duration}")
    Call<JoinMeetingResponse> joinMeeting(@Path("duration") int duration);

    /**
     * Leave the current room.
     * @param channelName name of the channel(meeting) to leave
     * @return a response from the backend
     */
    @POST("api/matching/leave/{channelName}")
    Call<LeaveMeetingResponse> leaveMeeting(@Path("channelName") String channelName);
}
