package com.example.studybuddyapp;

public final class AgoraConfig {

    // Agora App ID 
    public static final String APP_ID = "19c0c9217e3c429c9a3ce1839f92d90d";

    // temporary token for 'study_1' channel (set the 'custom' focus time as 1 min)
    public static final String TEMP_TOKEN = "007eJxTYFiTstMu9MW3cBW9Y2keP+bKZtyxZnwZpuIbwvvK7uGkxpkKDIaWyQbJlkaG5qnGySZGlsmWicbJqYYWxpZplkYplgYpkv92ZDYEMjI4ie9jYWSAQBCfnaG4pDSlMt6QgQEAjnYf9w==";

    private AgoraConfig() {}

    /**
     * builds a channel name from the selected focus duration.
     * This is a placeholder until the matching engine is implemented.
     * matching engine will replace this with server-assigned channel names.
     */
    public static String channelNameForDuration(int durationMinutes) {
        return "study_" + durationMinutes;
    }
}
