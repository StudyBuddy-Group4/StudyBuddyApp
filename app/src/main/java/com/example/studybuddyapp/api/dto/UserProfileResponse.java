package com.example.studybuddyapp.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the profile data returned for the current user.
 */
public class UserProfileResponse {
    // User id
    private Long id;
    // Username
    private String username;
    // Email address
    private String email;

    @SerializedName("admin")
    // Admin flag
    private boolean isAdmin;

    @SerializedName("banned")
    // Permanent ban flag
    private boolean isBanned;

    // Temporary restriction end
    private String bannedUntil;

    // Rating
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
        // Permanent bans always win, regardless of any temporary timestamp.
        if (isBanned) return true;
        // No temporary end time means there is no active temporary restriction to evaluate.
        if (bannedUntil == null || bannedUntil.isEmpty()) return false;
        try {
            // Temporary restrictions are active only while the ban end time is still in the future.
            java.time.LocalDateTime until = java.time.LocalDateTime.parse(bannedUntil);
            return until.isAfter(java.time.LocalDateTime.now());
        } catch (Exception e) {
            // Invalid timestamps are treated as non-restricting instead of crashing the caller.
            return false;
        }
    }
}
