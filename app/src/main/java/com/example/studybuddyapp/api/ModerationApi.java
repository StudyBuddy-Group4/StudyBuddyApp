package com.example.studybuddyapp.api;

import com.example.studybuddyapp.api.dto.ReportRequest;
import com.example.studybuddyapp.api.dto.ReportResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Moderation and report endpoints.
 */
public interface ModerationApi {

    // Submit a user report
    // Used by the participant flag flow.
    @POST("api/moderation/report")
    Call<Void> submitReport(@Body ReportRequest request);

    // Apply a moderation action to a user
    // Actions include warning, kick, and ban.
    @PUT("api/moderation/action/{userId}/{action}")
    Call<Void> applyAdminAction(@Path("userId") long reportedUserId,
                                @Path("action") String action);

    // Update report status
    // Admin review uses this to mark pending reports.
    @PUT("api/moderation/report/{reportId}/status/{status}")
    Call<Void> updateReportStatus(@Path("reportId") long reportId,
                                  @Path("status") String status);

    // Load all reports
    // Drives the admin moderation list.
    @GET("api/moderation/reports")
    Call<List<ReportResponse>> getAllReports();
}
