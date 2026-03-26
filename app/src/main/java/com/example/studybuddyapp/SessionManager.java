package com.example.studybuddyapp;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    // SharedPreferences file used for the current login session.
    private static final String PREF_NAME = "studybuddy_session";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_IS_ADMIN = "is_admin";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Stores the values we need to restore the signed-in user later.
    public void saveLoginSession(String token, long userId, String username, boolean isAdmin) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .putBoolean(KEY_IS_ADMIN, isAdmin)
                .apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, "");
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public boolean isAdmin() {
        return prefs.getBoolean(KEY_IS_ADMIN, false);
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }

    // Clears every stored session field during logout or account removal.
    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
