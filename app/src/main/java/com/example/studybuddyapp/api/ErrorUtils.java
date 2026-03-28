package com.example.studybuddyapp.api;

import java.io.IOException;

import retrofit2.Response;

/**
 * Small helpers for backend error messages.
 */
public final class ErrorUtils {

    private ErrorUtils() {}

    /**
     * Extracts a human-readable error message from a non-successful Retrofit response.
     * The backend may return a plain string or a JSON object with validation details.
     */
    public static String parseError(Response<?> response, String fallback) {
        // Without an error body there is nothing more specific to show.
        if (response.errorBody() == null) return fallback;
        try {
            // Read the body once and keep only the part that is safe to show in the UI.
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
                    // Short plain-text backend errors are usually already readable enough for the UI.
                    return body;
                }
            }
        } catch (IOException ignored) {
            // Fall back to the provided default message when the body cannot be read safely.
        }
        // Long or unreadable payloads are hidden behind the caller-provided fallback message.
        return fallback;
    }
}
