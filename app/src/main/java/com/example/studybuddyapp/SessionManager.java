package com.example.studybuddyapp;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Handles reading and writing the current user's session data.
 */
public class SessionManager {

    private static final String PREF_NAME = "studybuddy_session";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_IS_ADMIN = "is_admin";

    private final SharedPreferences prefs;

    /**
     * Creates a session manager backed by the app's shared preferences.
     */
    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Saves the values needed to restore the signed-in user later.
     */
    public void saveLoginSession(String token, long userId, String username, boolean isAdmin) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .putBoolean(KEY_IS_ADMIN, isAdmin)
                .apply();
    }

    /**
     * Returns the stored JWT token, or an empty string if none exists.
     */
    public String getToken() {
        return prefs.getString(KEY_TOKEN, "");
    }

    /**
     * Returns the stored user id, or -1 if no user is saved.
     */
    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }

    /**
     * Returns the stored username, or an empty string if none exists.
     */
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    /**
     * Returns whether the stored user is marked as an admin.
     */
    public boolean isAdmin() {
        return prefs.getBoolean(KEY_IS_ADMIN, false);
    }

    /**
     * Returns true when a non-empty token is stored.
     */
    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }

    /**
     * Clears all stored session data during logout or account removal.
     */
    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
