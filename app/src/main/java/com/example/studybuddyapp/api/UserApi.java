package com.example.studybuddyapp.api;

import com.example.studybuddyapp.api.dto.DeleteAccountRequest;
import com.example.studybuddyapp.api.dto.UpdateProfileRequest;
import com.example.studybuddyapp.api.dto.UserProfileResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.PUT;

/**
 * User-profile and account endpoints.
 */
public interface UserApi {

    // Load the current profile
    @GET("api/user/profile")
    Call<UserProfileResponse> getProfile();

    // Update profile fields
    @PUT("api/user/update")
    Call<String> updateProfile(@Body UpdateProfileRequest request);

    // Change password
    @PUT("api/user/change-password")
    Call<String> changePassword(@Body java.util.Map<String, String> request);

    // Delete the current account
    @HTTP(method = "DELETE", path = "api/user/delete", hasBody = true)
    Call<String> deleteAccount(@Body DeleteAccountRequest request);
}
