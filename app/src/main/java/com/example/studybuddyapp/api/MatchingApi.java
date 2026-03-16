package com.example.studybuddyapp.api;


import com.example.studybuddyapp.api.dto.JoinMeetingResponse;
import com.example.studybuddyapp.api.dto.LeaveMeetingResponse;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface MatchingApi {

    @POST("api/matching/join")
    Call<JoinMeetingResponse> join(@Query("duration") int duration);

    @POST("api/matching/leave")
    Call<LeaveMeetingResponse> leave(@Query("channelName") String channelName);
}
