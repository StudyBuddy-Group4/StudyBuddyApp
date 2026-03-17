package com.example.studybuddyapp;

public final class AgoraConfig {

    // Agora App ID 
    public static final String APP_ID = "19c0c9217e3c429c9a3ce1839f92d90d";

    // temporary token for 'study_15' channel (set the 'custom' focus time as 15 min)
    public static final String TEMP_TOKEN = "007eJxTYHigk6e2K/iespOhwSNb25bn9/gb1VRd7PaqL2T7f7JQrVSBwdAy2SDZ0sjQPNU42cTIMtky0Tg51dDC2DLN0ijF0iDFfO/OzIZARoZX9U3MjAwQCOKzMxSXlKZUxhsyMAAAEeUfCw==";

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
