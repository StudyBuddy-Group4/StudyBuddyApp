package com.example.studybuddyapp.api;

/**
 * Central place for backend base-url settings.
 */
public final class ApiConfig {

    // Railway remote server (for remote testing):
    public static final String BASE_URL = "https://studybuddy-backend-deploy-production.up.railway.app/";

    // Local development (emulator → host machine):
    // public static final String BASE_URL = "http://10.0.2.2:8080/";

    // No instances
    private ApiConfig() {}
}
