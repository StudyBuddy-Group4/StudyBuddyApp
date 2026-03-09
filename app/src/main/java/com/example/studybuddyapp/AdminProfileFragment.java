package com.example.studybuddyapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

public class AdminProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Load the new admin layout
        return inflater.inflate(R.layout.fragment_admin_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Standard actions
        view.findViewById(R.id.menu_edit_profile).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        view.findViewById(R.id.menu_security).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SecurityMenuActivity.class)));

        // ADMIN ONLY ACTION: Open History Reports
        view.findViewById(R.id.menu_history_reports).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), HistoryReportsActivity.class)));

        // SIMULATE FLAG NOTIFICATION
        view.findViewById(R.id.menu_simulate_alert).setOnClickListener(v -> {
            // Create a mock notification dialog
            new AlertDialog.Builder(requireContext())
                    .setTitle("🚨 Urgent: Participant Flagged!")
                    .setMessage("A user has been flagged for inappropriate behavior in Room #104. Immediate review is requested.")
                    .setCancelable(false)
                    .setPositiveButton("Enter Admin View", (dialog, which) -> {
                        // Route to the Admin Meeting Room!
                        startActivity(new Intent(requireContext(), AdminMeetingRoomActivity.class));
                    })
                    .setNegativeButton("Dismiss", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        view.findViewById(R.id.menu_logout).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LaunchOptionsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}