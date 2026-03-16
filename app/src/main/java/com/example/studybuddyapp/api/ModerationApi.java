package com.example.studybuddyapp.api;

import com.example.studybuddyapp.api.dto.ReportRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Retrofit API Interface for all Moderation Engine network calls.
 * This interface defines the endpoints required for reporting users,
 * fetching report history, and applying administrative punishments.
 * Keeping this interface separate ensures our classes remain under
 * the 400 lines of code limit threshold.
 */
public interface ModerationApi {

    /**
     * Submits a new report for inappropriate behavior [MOD-1, MOD-3].
     * @param request The ReportRequest payload containing the report details.
     * @return A Retrofit Call object for the network request.
     */
    @POST("api/moderation/report")
    Call<Void> submitReport(@Body ReportRequest request);

    /**
     * Applies a punishment or dismisses a report [MOD-5, MOD-7, MOD-8, MOD-10].
     * @param reportedUserId The user receiving the punishment.
     * @param action The action to take: "KICK", "BAN_3_DAYS", "BAN_PERMANENT", or "DISMISS".
     * @return A Retrofit Call object for the network request.
     */
    @PUT("api/moderation/action/{userId}/{action}")
    Call<Void> applyAdminAction(@Path("userId") long reportedUserId, @Path("action") String action);
    
    /**
     * Fetches all reports from the backend for the admin to review [MOD-6].
     * @return A List of ReportResponse objects.
     */
    @GET("api/moderation/reports")
    Call<List<ReportResponse>> getAllReports();
}