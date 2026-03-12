package com.example.studybuddyapp.api;

import com.example.studybuddyapp.api.dto.LoginRequest;
import com.example.studybuddyapp.api.dto.LoginResponse;
import com.example.studybuddyapp.api.dto.RegisterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {

    @POST("api/auth/register")
    Call<String> register(@Body RegisterRequest request);

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/auth/logout")
    Call<String> logout();
}
