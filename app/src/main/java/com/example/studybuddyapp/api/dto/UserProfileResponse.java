package com.example.studybuddyapp.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the profile data returned for the current user.
 */
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;

    @SerializedName("admin")
    private boolean isAdmin;

    @SerializedName("banned")
    private boolean isBanned;

    private String bannedUntil;

    private double rating;

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public boolean isAdmin() { return isAdmin; }
    public boolean isBanned() { return isBanned; }
    public String getBannedUntil() { return bannedUntil; }
    public double getRating() { return rating; }

    /**
     * Returns whether the user is currently blocked from joining sessions.
     */
    public boolean isCurrentlyRestricted() {
        if (isBanned) return true;
        if (bannedUntil == null || bannedUntil.isEmpty()) return false;
        try {
            // Temporary restrictions are active only while the ban end time is still in the future.
            java.time.LocalDateTime until = java.time.LocalDateTime.parse(bannedUntil);
            return until.isAfter(java.time.LocalDateTime.now());
        } catch (Exception e) {
            return false;
        }
    }
}
