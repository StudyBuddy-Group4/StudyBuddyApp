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

public class AdminProfileFragment extends Fragment {

    private static final long POLL_INTERVAL_MS = 5000;

    private TextView tvName;
    private TextView tvId;
    private Handler pollHandler;
    private final Set<Long> seenReportIds = new HashSet<>();
    private boolean isShowingAlert = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvName = view.findViewById(R.id.tvAdminProfileName);
        tvId = view.findViewById(R.id.tvAdminProfileId);

        showCachedData();

        view.findViewById(R.id.menu_edit_profile).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        view.findViewById(R.id.menu_security).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SecurityMenuActivity.class)));

        view.findViewById(R.id.menu_history_reports).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), HistoryReportsActivity.class)));

        view.findViewById(R.id.menu_logout).setOnClickListener(v -> {
            stopPolling();
            new SessionManager(requireContext()).clearSession();
            ApiClient.resetInstance();
            Intent intent = new Intent(requireContext(), LaunchOptionsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileFromBackend();
        startPolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPolling();
    }

    private void showCachedData() {
        SessionManager session = new SessionManager(requireContext());
        String username = session.getUsername();
        if (username != null && !username.isEmpty()) {
            tvName.setText(username);
        }
        long userId = session.getUserId();
        if (userId > 0) {
            tvId.setText("ID: " + userId);
        }
    }

    private void loadProfileFromBackend() {
        UserApi api = ApiClient.getUserApi(requireContext());
        api.getProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call,
                                   Response<UserProfileResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    UserProfileResponse profile = response.body();
                    tvName.setText(profile.getUsername());
                    tvId.setText("ID: " + profile.getId());

                    SessionManager session = new SessionManager(requireContext());
                    session.saveLoginSession(session.getToken(), profile.getId(),
                            profile.getUsername(), profile.isAdmin());
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) { }
        });
    }

    private void startPolling() {
        pollHandler = new Handler(Looper.getMainLooper());
        pollHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;
                pollForNewReports();
                pollHandler.postDelayed(this, POLL_INTERVAL_MS);
            }
        }, POLL_INTERVAL_MS);
    }

    private void stopPolling() {
        if (pollHandler != null) {
            pollHandler.removeCallbacksAndMessages(null);
            pollHandler = null;
        }
    }

    private void pollForNewReports() {
        if (isShowingAlert) return;

        ModerationApi api = ApiClient.getModerationApi(requireContext());
        api.getAllReports().enqueue(new Callback<List<ReportResponse>>() {
            @Override
            public void onResponse(Call<List<ReportResponse>> call,
                                   Response<List<ReportResponse>> response) {
                if (!isAdded() || isShowingAlert) return;
                if (response.isSuccessful() && response.body() != null) {
                    for (ReportResponse r : response.body()) {
                        if ("PENDING".equals(r.getStatus()) && !seenReportIds.contains(r.getId())) {
                            seenReportIds.add(r.getId());
                            showFlagNotification(r);
                            return;
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ReportResponse>> call, Throwable t) { }
        });
    }

    private void showFlagNotification(ReportResponse report) {
        if (!isAdded()) return;
        isShowingAlert = true;

        String reportedName = report.getReportedUsername() != null
                ? report.getReportedUsername() : "User #" + report.getReportedUserId();
        String message = "Flagged User Id: " + report.getReportedUserId()
                + "\nFor: " + report.getReason();

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.flag_notification_title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_go_to_meeting, (dialog, which) -> {
                    isShowingAlert = false;
                    Intent intent = new Intent(requireContext(), AdminMeetingRoomActivity.class);
                    intent.putExtra("channel_name", report.getMeetingId());
                    intent.putExtra("report_id", report.getId());
                    intent.putExtra("reported_user_id", report.getReportedUserId());
                    startActivity(intent);
                })
                .setNegativeButton(R.string.btn_dismiss, (dialog, which) -> {
                    isShowingAlert = false;
                    dismissReport(report.getId());
                })
                .show();
    }

    private void dismissReport(long reportId) {
        ModerationApi api = ApiClient.getModerationApi(requireContext());
        api.updateReportStatus(reportId, "DISMISSED").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) { }
            @Override
            public void onFailure(Call<Void> call, Throwable t) { }
        });
    }
}
