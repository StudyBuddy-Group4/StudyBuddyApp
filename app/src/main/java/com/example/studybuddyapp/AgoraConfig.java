package com.example.studybuddyapp;

/**
 * Stores Agora-related constants and fallback channel naming logic.
 */
public final class AgoraConfig {

    // Agora App ID 
    public static final String APP_ID = "0758666b7db245fc95b5b58ea5ba274c";

    // Token is empty for testing mode (Agora project must have "No Certificate" or testing mode enabled)
    public static final String TEMP_TOKEN = "";

    private AgoraConfig() {}

    /**
     * Fallback channel name if the matching engine is unavailable.
     */
    public static String channelNameForDuration(int durationMinutes) {
        return "study_" + durationMinutes;
    }
}
