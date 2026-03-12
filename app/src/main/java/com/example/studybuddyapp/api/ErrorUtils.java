package com.example.studybuddyapp.api;

import java.io.IOException;

import retrofit2.Response;

public final class ErrorUtils {

    private ErrorUtils() {}

    /**
     * Extracts a human-readable error message from a non-successful Retrofit response.
     * The backend may return a plain string or a JSON object with validation details.
     */
    public static String parseError(Response<?> response, String fallback) {
        if (response.errorBody() == null) return fallback;
        try {
            String body = response.errorBody().string();
            if (body != null && !body.isEmpty()) {
                // If body contains "default message [", extract the readable part
                if (body.contains("default message [")) {
                    int lastIdx = body.lastIndexOf("default message [");
                    if (lastIdx >= 0) {
                        int start = lastIdx + "default message [".length();
                        int end = body.indexOf("]", start);
                        if (end > start) {
                            return body.substring(start, end);
                        }
                    }
                }
                // Try to use the body directly if it's short enough
                if (body.length() < 200) {
                    return body;
                }
            }
        } catch (IOException ignored) {
            // fall through
        }
        return fallback;
    }
}
