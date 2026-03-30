package com.example.studybuddyapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.MatchingApi;
import com.example.studybuddyapp.api.UserApi;
import com.example.studybuddyapp.api.dto.JoinMeetingResponse;
import com.example.studybuddyapp.api.dto.UserProfileResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Lets the user choose a focus duration and start the matching flow.
 */
public class HomeFragment extends Fragment {

    // These three chips mirror the available focus-time choices shown on the home screen.
    private TextView chip15, chip30, chipCustom;
    // Keep the selected duration in memory so the same value can be reused by dialogs and matching.
    private int selectedDurationMinutes = 15;
    // Track whether the custom chip currently represents a user-entered duration.
    private boolean isCustomSelected = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // The home screen is lightweight enough to be fully recreated from its XML layout each time.
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind the selectable time chips and the primary action button for this screen.
        chip15 = view.findViewById(R.id.chip_15_min);
        chip30 = view.findViewById(R.id.chip_30_min);
        chipCustom = view.findViewById(R.id.chip_custom);
        Button btnStart = view.findViewById(R.id.btn_start);

        // The screen starts on the 15-minute preset.
        chip15.setOnClickListener(v -> {
            // Preset chips clear any previous custom value.
            selectedDurationMinutes = 15;
            isCustomSelected = false;
            chipCustom.setText(getString(R.string.time_custom));
            selectChip(chip15);
        });

        chip30.setOnClickListener(v -> {
            // Preset chips restore the default custom label text.
            selectedDurationMinutes = 30;
            isCustomSelected = false;
            chipCustom.setText(getString(R.string.time_custom));
            selectChip(chip30);
        });

        // Custom duration entry is handled in a dialog so the main screen stays simple.
        chipCustom.setOnClickListener(v -> showCustomMinutesDialog());

        btnStart.setOnClickListener(v -> {
            // Guard against invalid state before the user enters the matching flow.
            if (selectedDurationMinutes <= 0) {
                Toast.makeText(requireContext(),
                        "Please enter a valid number of minutes.", Toast.LENGTH_SHORT).show();
                return;
            }
            // The confirmation step gives the user one last chance to add tasks first.
            showReadyToFocusDialog();
        });
    }

    /**
     * Updates the selected time chip so only one option appears active.
     */
    private void selectChip(TextView selected) {
        TextView[] chips = {chip15, chip30, chipCustom};
        for (TextView chip : chips) {
            if (chip == selected) {
                // Highlight only the active chip so the current duration is obvious.
                chip.setBackgroundResource(R.drawable.bg_time_chip_selected);
                chip.setTextColor(getResources().getColor(R.color.white, null));
            } else {
                // Reset all non-selected chips back to the default appearance.
                chip.setBackgroundResource(R.drawable.bg_time_chip);
                chip.setTextColor(getResources().getColor(R.color.dark_text, null));
            }
        }
    }

    /**
     * Opens a dialog for entering a custom focus duration.
     */
    private void showCustomMinutesDialog() {
        final EditText input = new EditText(requireContext());
        input.setHint("Enter minutes");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setPadding(50, 30, 50, 30);

        // Keep custom duration entry in a simple modal instead of crowding the home screen layout.
        new AlertDialog.Builder(requireContext())
                .setTitle("Custom focus time")
                .setMessage("Enter the number of minutes:")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    // Read the dialog field only when the user confirms.
                    String value = input.getText().toString().trim();

                    // Stop early when the user closes the dialog without entering a number.
                    if (value.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "Please enter a number.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int minutes = Integer.parseInt(value);

                    // Custom durations still need to be positive before we update the UI.
                    if (minutes <= 0) {
                        Toast.makeText(requireContext(),
                                "Minutes must be greater than 0.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Save the custom value and make the custom chip look selected.
                    selectedDurationMinutes = minutes;
                    isCustomSelected = true;
                    // Replace the placeholder text with the exact custom duration the user entered.
                    chipCustom.setText(minutes + " min");
                    selectChip(chipCustom);
                })
                // Cancel leaves the previous duration untouched.
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows the confirmation dialog before entering a focus session.
     */
    private void showReadyToFocusDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_ready_to_focus, null);

        // This modal mirrors the actual user decision point before matchmaking begins.
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            // The layout already contains its own rounded background.
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btn_add_task).setOnClickListener(v -> {
            dialog.dismiss();

            // Reuse the existing tasks tab instead of opening a separate screen.
            if (getActivity() instanceof MainHubActivity) {
                ((MainHubActivity) getActivity()).switchToTasksTab();
            }
        });

        dialogView.findViewById(R.id.btn_start_anyway).setOnClickListener(v -> {
            // Starting anyway continues into the account-status check and matching flow.
            // The dialog is only a checkpoint, not a separate stateful screen.
            dialog.dismiss();
            checkUserStatusAndLaunch();
        });

        dialog.show();
    }

    /**
     * Checks whether the user is allowed to join a session before matching them.
     */
    private void checkUserStatusAndLaunch() {
        // Always verify the latest restriction state before sending the user into a meeting room.
        UserApi api = ApiClient.getUserApi(requireContext());

        api.getProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call,
                                   Response<UserProfileResponse> response) {
                // Stop if the fragment is no longer attached to a valid activity.
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    UserProfileResponse profile = response.body();

                    // Restricted users get a warning modal instead of entering the matching queue.
                    if (profile.isCurrentlyRestricted()) {
                        showBannedNotificationModal(profile);
                    } else {
                        // Only unrestricted users continue into the actual meeting lookup.
                        launchMeetingRoom();
                    }
                } else {
                    // A failed profile lookup blocks the flow because restriction status is unknown.
                    Toast.makeText(requireContext(),
                            "Failed to verify account status.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                // Network failures also block the flow because the restriction check could not finish.
                if (!isAdded()) return;
                Toast.makeText(requireContext(),
                        "Network error checking status.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Explains why the user cannot start a focus session right now.
     */
    private void showBannedNotificationModal(UserProfileResponse profile) {
        String message;
        if (profile.isBanned()) {
            // Permanent bans use a fixed explanation because there is no end date to show.
            message = "Your account has been permanently banned due to inappropriate behavior.";
        } else {
            // Temporary restrictions include the backend expiry value when it is available.
            message = "Your account is temporarily restricted until "
                    + (profile.getBannedUntil() != null ? profile.getBannedUntil() : "unknown")
                    + " due to inappropriate behavior.";
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_exit_warning, null);

        TextView tvMessage = dialogView.findViewById(R.id.tvWarningMessage);
        if (tvMessage != null) {
            // Reuse the shared warning dialog and swap in the account-specific restriction message.
            tvMessage.setText(message);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            // The warning dialog keeps the same transparent window treatment as the other modals in this flow.
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // This dialog only warns the user, so the leave-session action is hidden.
        View btnBack = dialogView.findViewById(R.id.btnContinueStudying);
        if (btnBack != null) {
            // The only action available here is dismissing the warning.
            btnBack.setOnClickListener(v -> dialog.dismiss());
        }

        View btnLeave = dialogView.findViewById(R.id.btnLeaveSession);
        if (btnLeave != null) {
            // This shared dialog layout normally has a second action, but restrictions only allow acknowledgement.
            btnLeave.setVisibility(View.GONE);
        }

        dialog.show();
    }

    /**
     * Starts the meeting flow with the currently selected focus duration.
     */
    private void launchMeetingRoom() {
        // Ask the backend for a room that matches the selected focus duration.
        MatchingApi matchingApi = ApiClient.getMatchingApi(requireContext());
        matchingApi.joinMeeting(selectedDurationMinutes).enqueue(new Callback<JoinMeetingResponse>() {
            @Override
            public void onResponse(Call<JoinMeetingResponse> call,
                                   Response<JoinMeetingResponse> response) {
                // Ignore late callbacks after the fragment has been detached.
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    JoinMeetingResponse meeting = response.body();
                    // The meeting screen reads both extras on startup.
                    Intent intent = new Intent(requireContext(), MeetingRoomActivity.class);

                    // Pass the duration and backend-provided channel into the meeting screen.
                    intent.putExtra(MeetingRoomActivity.EXTRA_FOCUS_DURATION, selectedDurationMinutes);
                    intent.putExtra(MeetingRoomActivity.EXTRA_CHANNEL_NAME, meeting.getChannelName());
                    // MeetingRoomActivity relies on these extras to rebuild the room after recreation.
                    startActivity(intent);
                } else {
                    // Keep the failure message simple so the user can retry immediately.
                    Toast.makeText(requireContext(),
                            "Failed to find a meeting. Please try again.",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JoinMeetingResponse> call, Throwable t) {
                // Transport errors leave the user on the home screen with their chosen duration intact.
                if (!isAdded()) return;
                Toast.makeText(requireContext(),
                        "Network error. Please try again.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
