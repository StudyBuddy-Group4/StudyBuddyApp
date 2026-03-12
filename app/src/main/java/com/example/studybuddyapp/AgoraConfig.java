package com.example.studybuddyapp;

/**
 * Agora credentials and channel configuration.
 *
 * HOW TO SET UP:
 * 1. Go to https://console.agora.io and sign in.
 * 2. Create a project (or open your existing one).
 * 3. Copy the App ID and paste it into APP_ID below.
 * 4. For testing: generate a temporary token in the Console
 *    using channel name "study_15" (or leave TEMP_TOKEN empty
 *    if your project has no certificate / is in testing mode).
 */
public final class AgoraConfig {

    // TODO: Paste your Agora App ID here
    public static final String APP_ID = "19c0c9217e3c429c9a3ce1839f92d90d";

    // TODO: Paste a temporary token here (leave empty for APP ID-only auth)
    public static final String TEMP_TOKEN = "007eJxTYPCVroxae+Se6B/n2S77itcF9X1wd1WLKqg+U5XhWN6mY6zAYGiZbJBsaWRonmqcbGJkmWyZaJycamhhbJlmaZRiaZCyiHt9ZkMgI4PAzIVMjAwQCOJzMBSXlKZUxhuaMjAAAHTLH0o=";

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
