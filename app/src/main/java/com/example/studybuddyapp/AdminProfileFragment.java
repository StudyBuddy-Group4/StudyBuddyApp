package com.example.studybuddyapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.studybuddyapp.api.ApiClient;

public class AdminProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SessionManager session = new SessionManager(requireContext());
        TextView tvName = view.findViewById(R.id.tvAdminProfileName);
        TextView tvId = view.findViewById(R.id.tvAdminProfileId);

        String username = session.getUsername();
        if (username != null && !username.isEmpty()) {
            tvName.setText(username);
        }
        long userId = session.getUserId();
        if (userId > 0) {
            tvId.setText("ID: " + userId);
        }

        view.findViewById(R.id.menu_edit_profile).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        view.findViewById(R.id.menu_security).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SecurityMenuActivity.class)));

        view.findViewById(R.id.menu_history_reports).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), HistoryReportsActivity.class)));

        view.findViewById(R.id.menu_simulate_alert).setOnClickListener(v ->
                showFlagNotificationDialog());

        view.findViewById(R.id.menu_logout).setOnClickListener(v -> {
            session.clearSession();
            ApiClient.resetInstance();
            Intent intent = new Intent(requireContext(), LaunchOptionsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void showFlagNotificationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.flag_notification_title)
                .setMessage("Flagged User Id : 25020098\nFor: Nudity")
                .setCancelable(false)
                .setPositiveButton(R.string.btn_go_to_meeting, (dialog, which) ->
                        startActivity(new Intent(requireContext(), AdminMeetingRoomActivity.class)))
                .setNegativeButton(R.string.btn_dismiss, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
