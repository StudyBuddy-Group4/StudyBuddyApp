package com.example.studybuddyapp;

public final class AgoraConfig {

    // Agora App ID 
    public static final String APP_ID = "19c0c9217e3c429c9a3ce1839f92d90d";

    // temporary token for 'study_1' channel (set the 'custom' focus time as 1 min)
    public static final String TEMP_TOKEN = "007eJxTYFida22jEfGET48rSD0qbWGb7p3proEPYub//XBMemeb/DUFBkPLZINkSyND81TjZBMjy2TLROPkVEMLY8s0S6MUS4OUe2E7MxsCGRlYtzOxMDJAIIjPwVBcUppSGW9oysAAAFEjH08=";

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
