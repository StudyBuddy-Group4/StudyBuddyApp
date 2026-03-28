package com.example.studybuddyapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.ModerationApi;
import com.example.studybuddyapp.api.UserApi;
import com.example.studybuddyapp.api.dto.ReportResponse;
import com.example.studybuddyapp.api.dto.UserProfileResponse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Shows the admin profile screen and polls for new pending reports.
 */
public class AdminProfileFragment extends Fragment {

    // Report poll interval
    private static final long POLL_INTERVAL_MS = 5000;

    // Profile header labels
    private TextView tvName;
    private TextView tvId;
    // Main-thread poll handler
    private Handler pollHandler;
    // Reports already shown in a popup
    private final Set<Long> seenReportIds = new HashSet<>();
    // Only one alert at a time
    private boolean isShowingAlert = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the admin profile layout
        return inflater.inflate(R.layout.fragment_admin_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind the profile header
        tvName = view.findViewById(R.id.tvAdminProfileName);
        tvId = view.findViewById(R.id.tvAdminProfileId);

        // Show cached session data first
        showCachedData();

        // Edit profile
        view.findViewById(R.id.menu_edit_profile).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        // Security menu
        view.findViewById(R.id.menu_security).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SecurityMenuActivity.class)));

        // Report history screen
        view.findViewById(R.id.menu_history_reports).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), HistoryReportsActivity.class)));

        view.findViewById(R.id.menu_logout).setOnClickListener(v -> {
            // Stop report checks before leaving
            stopPolling();
            // Clear local session state
            new SessionManager(requireContext()).clearSession();
            ApiClient.resetInstance();
            Intent intent = new Intent(requireContext(), LaunchOptionsActivity.class);
            // Prevent returning to admin screens
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh profile data
        loadProfileFromBackend();
        // Resume report polling
        startPolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Pause report polling off-screen
        stopPolling();
    }

    /**
     * Fills the header from the stored session.
     */
    private void showCachedData() {
        SessionManager session = new SessionManager(requireContext());
        // Cached username
        String username = session.getUsername();
        if (username != null && !username.isEmpty()) {
            tvName.setText(username);
        }
        // Cached user id
        long userId = session.getUserId();
        if (userId > 0) {
            tvId.setText("ID: " + userId);
        }
    }

    /**
     * Refreshes the admin profile from the backend.
     */
    private void loadProfileFromBackend() {
        UserApi api = ApiClient.getUserApi(requireContext());
        api.getProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call,
                                   Response<UserProfileResponse> response) {
                // Ignore late callbacks
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    UserProfileResponse profile = response.body();
                    // Update the header
                    tvName.setText(profile.getUsername());
                    tvId.setText("ID: " + profile.getId());

                    // Keep session data in sync
                    SessionManager session = new SessionManager(requireContext());
                    session.saveLoginSession(session.getToken(), profile.getId(),
                            profile.getUsername(), profile.isAdmin());
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                // Keep cached values
            }
        });
    }

    /**
     * Starts the repeating report poll.
     */
    private void startPolling() {
        pollHandler = new Handler(Looper.getMainLooper());
        pollHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Stop when the fragment is gone
                if (!isAdded()) return;
                // Check for new reports
                pollForNewReports();
                // Schedule the next check
                pollHandler.postDelayed(this, POLL_INTERVAL_MS);
            }
        }, POLL_INTERVAL_MS);
    }

    /**
     * Stops the repeating report poll.
     */
    private void stopPolling() {
        if (pollHandler != null) {
            // Clear all pending callbacks
            pollHandler.removeCallbacksAndMessages(null);
            pollHandler = null;
        }
    }

    /**
     * Looks for the next unseen pending report.
     */
    private void pollForNewReports() {
        // Do not stack alerts
        if (isShowingAlert) return;

        ModerationApi api = ApiClient.getModerationApi(requireContext());
        api.getAllReports().enqueue(new Callback<List<ReportResponse>>() {
            @Override
            public void onResponse(Call<List<ReportResponse>> call,
                                   Response<List<ReportResponse>> response) {
                // Ignore late callbacks or overlap with an open alert
                if (!isAdded() || isShowingAlert) return;
                if (response.isSuccessful() && response.body() != null) {
                    // Show only the first unseen pending report
                    for (ReportResponse r : response.body()) {
                        if ("PENDING".equals(r.getStatus()) && !seenReportIds.contains(r.getId())) {
                            // Mark it as already shown locally
                            seenReportIds.add(r.getId());
                            showFlagNotification(r);
                            return;
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ReportResponse>> call, Throwable t) {
                // Try again on the next poll
            }
        });
    }

    /**
     * Shows a popup for one pending report.
     */
    private void showFlagNotification(ReportResponse report) {
        // Ignore calls after detach
        if (!isAdded()) return;
        // Block additional alerts while this one is visible
        isShowingAlert = true;

        String reportedName = report.getReportedUsername() != null
                ? report.getReportedUsername() : "User #" + report.getReportedUserId();
        // Use a fallback label when the username is missing
        String message = "Flagged User Id: " + report.getReportedUserId()
                + "\nFor: " + report.getReason();

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.flag_notification_title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_go_to_meeting, (dialog, which) -> {
                    // Allow future alerts once this one is handled
                    isShowingAlert = false;
                    Intent intent = new Intent(requireContext(), AdminMeetingRoomActivity.class);
                    // Pass the room and report info into the admin room
                    intent.putExtra("channel_name", report.getMeetingId());
                    intent.putExtra("report_id", report.getId());
                    intent.putExtra("reported_user_id", report.getReportedUserId());
                    startActivity(intent);
                })
                .setNegativeButton(R.string.btn_dismiss, (dialog, which) -> {
                    // Allow future alerts once this one is dismissed
                    isShowingAlert = false;
                    // Mark the report as dismissed in the backend
                    dismissReport(report.getId());
                })
                .show();
    }

    /**
     * Marks a report as dismissed.
     */
    private void dismissReport(long reportId) {
        ModerationApi api = ApiClient.getModerationApi(requireContext());
        api.updateReportStatus(reportId, "DISMISSED").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // No extra UI here
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Ignore and continue polling
            }
        });
    }
}
