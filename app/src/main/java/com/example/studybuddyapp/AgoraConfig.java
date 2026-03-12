package com.example.studybuddyapp;

public final class AgoraConfig {

    // Agora App ID 
    public static final String APP_ID = "19c0c9217e3c429c9a3ce1839f92d90d";

    // temporary token for 'study_15' channel (leave empty for APP ID-only auth)
    public static final String TEMP_TOKEN = "007eJxTYPixKehm4tJ7du82sv68/u2X6YFHGoWVT70YwzZ/nDnz/bn3CgyGlskGyZZGhuapxskmRpbJlonGyamGFsaWaZZGKZYGKQXbN2U2BDIyPD46hZmRAQJBfA6G4pLSlMp4Q1MGBgB6giZY";

    private AgoraConfig() {}

    /**
     * Builds a channel name from the selected focus duration.
     * This is a placeholder until the matching engine is implemented.
     * The matching engine will replace this with server-assigned channel names.
     */
    public static String channelNameForDuration(int durationMinutes) {
        return "study_" + durationMinutes;
    }
}
