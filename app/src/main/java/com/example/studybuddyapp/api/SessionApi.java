package com.example.studybuddyapp.api;

import com.example.studybuddyapp.api.dto.SessionHistoryItem;
import com.example.studybuddyapp.api.dto.SessionStatistics;
import com.example.studybuddyapp.api.dto.StartSessionRequest;
import com.example.studybuddyapp.api.dto.StartSessionResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Focus-session and statistics endpoints.
 */
public interface SessionApi {

    // Start a backend session
    @POST("api/session/start")
    Call<StartSessionResponse> startSession(@Body StartSessionRequest request);

    // Mark a session as complete
    @PUT("api/session/{id}/complete")
    Call<String> completeSession(@Path("id") long sessionId);

    // Mark a session as incomplete
    @PUT("api/session/{id}/incomplete")
    Call<String> incompleteSession(@Path("id") long sessionId);

    // Load session history
    @GET("api/session/history")
    Call<List<SessionHistoryItem>> getHistory();

    // Load aggregate statistics
    @GET("api/session/statistics")
    Call<SessionStatistics> getStatistics();
}
