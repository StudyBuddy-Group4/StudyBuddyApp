package com.example.studybuddyapp.api;


import com.example.studybuddyapp.api.dto.JoinMeetingResponse;
import com.example.studybuddyapp.api.dto.LeaveMeetingResponse;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface MatchingApi {

    @POST("api/matching/join/{duration}")
    Call<JoinMeetingResponse> join(@Path("duration") int duration);

    @POST("api/matching/leave/{channelName}")
    Call<LeaveMeetingResponse> leave(@Path("channelName") String channelName);
}
