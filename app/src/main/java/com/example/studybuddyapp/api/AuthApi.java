package com.example.studybuddyapp.api;

import com.example.studybuddyapp.api.dto.LoginRequest;
import com.example.studybuddyapp.api.dto.LoginResponse;
import com.example.studybuddyapp.api.dto.RegisterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Authentication endpoints.
 */
public interface AuthApi {

    // Register a new account
    @POST("api/auth/register")
    Call<String> register(@Body RegisterRequest request);

    // Log in with username/email and password
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    // Log out the current user
    @POST("api/auth/logout")
    Call<String> logout();
}
