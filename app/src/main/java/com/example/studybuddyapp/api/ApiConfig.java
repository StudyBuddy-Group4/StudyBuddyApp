package com.example.studybuddyapp.api;

public final class ApiConfig {

    /**
     * Base URL for Raye's Spring Boot backend.
     * - Emulator: use 10.0.2.2 (maps to host machine's localhost)
     * - Physical device on same WiFi: use your computer's LAN IP (e.g. 192.168.x.x)
     */
    // Remote server (switch to this for final demo):
    public static final String BASE_URL = "https://209.38.55.71/app/";

    // Local development (emulator → host machine):
    // public static final String BASE_URL = "http://10.0.2.2:8080/";

    private ApiConfig() {}
}
