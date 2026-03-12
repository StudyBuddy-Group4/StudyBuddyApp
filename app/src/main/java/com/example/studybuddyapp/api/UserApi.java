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

public interface UserApi {

    @GET("api/user/profile")
    Call<UserProfileResponse> getProfile();

    @PUT("api/user/update")
    Call<String> updateProfile(@Body UpdateProfileRequest request);

    @HTTP(method = "DELETE", path = "api/user/delete", hasBody = true)
    Call<String> deleteAccount(@Body DeleteAccountRequest request);
}
