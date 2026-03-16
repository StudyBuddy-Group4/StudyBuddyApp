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
import com.example.studybuddyapp.api.UserApi;
import com.example.studybuddyapp.api.dto.UserProfileResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * The HomeFragment is the primary dashboard where users can select their
 * desired study duration and initiate a focus session.
 * * It includes logic to intercept the session start if the user has been
 * restricted or banned by an administrator, fulfilling moderation requirements.
 */
public class HomeFragment extends Fragment {

    // UI elements for selecting the focus duration
    private TextView chip15, chip30, chipCustom;
    
    // Default focus duration is set to 15 minutes
    private int selectedDurationMinutes = 15;
    
    // Flag to track if the user has opted for a custom time input
    private boolean isCustomSelected = false;

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored.
     * Sets up the click listeners for the duration chips and the start button.
     *
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind the UI elements from the layout
        chip15 = view.findViewById(R.id.chip_15_min);
        chip30 = view.findViewById(R.id.chip_30_min);
        chipCustom = view.findViewById(R.id.chip_custom);
        Button btnStart = view.findViewById(R.id.btn_start);

        // Handle 15-minute selection
        chip15.setOnClickListener(v -> {
            selectedDurationMinutes = 15;
            isCustomSelected = false;
            chipCustom.setText(getString(R.string.time_custom));
            selectChip(chip15);
        });

        // Handle 30-minute selection
        chip30.setOnClickListener(v -> {
            selectedDurationMinutes = 30;
            isCustomSelected = false;
            chipCustom.setText(getString(R.string.time_custom));
            selectChip(chip30);
        });

        // Handle custom time selection by showing an input dialog
        chipCustom.setOnClickListener(v -> showCustomMinutesDialog());

        // Validate the selected time before prompting the user to start
        btnStart.setOnClickListener(v -> {
            if (selectedDurationMinutes <= 0) {
                Toast.makeText(requireContext(),
                        "Please enter a valid number of minutes.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Ask if they want to add a task before verifying their account status
            showReadyToFocusDialog();
        });
    }

    /**
     * Visually highlights the selected duration chip and resets the others.
     *
     * @param selected The TextView chip that the user just clicked.
     */
    private void selectChip(TextView selected) {
        TextView[] chips = {chip15, chip30, chipCustom};
        for (TextView chip : chips) {
            if (chip == selected) {
                // Apply the active styling
                chip.setBackgroundResource(R.drawable.bg_time_chip_selected);
                chip.setTextColor(getResources().getColor(R.color.white, null));
            } else {
                // Apply the inactive styling
                chip.setBackgroundResource(R.drawable.bg_time_chip);
                chip.setTextColor(getResources().getColor(R.color.dark_text, null));
            }
        }
    }

    /**
     * Displays an AlertDialog allowing the user to manually type in a custom
     * number of minutes for their study session.
     */
    private void showCustomMinutesDialog() {
        final EditText input = new EditText(requireContext());
        input.setHint("Enter minutes");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setPadding(50, 30, 50, 30);

        new AlertDialog.Builder(requireContext())
                .setTitle("Custom focus time")
                .setMessage("Enter the number of minutes:")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "Please enter a number.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    int minutes = Integer.parseInt(value);
                    if (minutes <= 0) {
                        Toast.makeText(requireContext(),
                                "Minutes must be greater than 0.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Update state variables with the custom input
                    selectedDurationMinutes = minutes;
                    isCustomSelected = true;
                    chipCustom.setText(minutes + " min");
                    selectChip(chipCustom);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Displays a nudge dialog asking the user if they want to document a task
     * before starting the session.
     */
    private void showReadyToFocusDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_ready_to_focus, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Redirect the user to the Task List tab to add a task
        dialogView.findViewById(R.id.btn_add_task).setOnClickListener(v -> {
            dialog.dismiss();
            if (getActivity() instanceof MainHubActivity) {
                ((MainHubActivity) getActivity()).switchToTasksTab();
            }
        });

        // Proceed to check authorization and launch the session
        dialogView.findViewById(R.id.btn_start_anyway).setOnClickListener(v -> {
            dialog.dismiss();
            checkUserStatusAndLaunch();
        });

        dialog.show();
    }

    /**
     * Triggers a network call to verify the user's account status before launching.
     * This ensures that banned or restricted users cannot join meetings, fulfilling
     * moderation requirements [MOD-9] and [MOD-11].
     */
    private void checkUserStatusAndLaunch() {
        UserApi api = ApiClient.getUserApi(requireContext());

        // Fetch the latest profile data from the backend using getProfile()
        api.getProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {

                    // Check if the user is banned using your isBanned() method
                    boolean isUserBanned = response.body().isBanned();

                    // Block entry if the account is currently banned
                    if (isUserBanned) {
                        showBannedNotificationModal();
                    } else {
                        // User is in good standing, proceed to the meeting room
                        launchMeetingRoom();
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to verify account status.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                Toast.makeText(requireContext(), "Network error checking status.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Displays a modal informing the user that they cannot enter the meeting room
     * because their account has been penalized by an administrator.
     */
    private void showBannedNotificationModal() {
        // This inflates UI-5.2-J: User Removal Notification Modal
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_exit_warning, null); // Substitute with your actual banned layout XML

        // Locate the text view and set the banned message
        TextView tvMessage = dialogView.findViewById(R.id.tvWarningMessage);
        if(tvMessage != null) {
            tvMessage.setText("Your account is currently restricted due to inappropriate behavior.");
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false) // Force the user to acknowledge
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Close the dialog and leave the user on the Home Fragment
        View btnBack = dialogView.findViewById(R.id.btnContinueStudying); // Reusing button ID from generic dialog
        if(btnBack != null) {
            btnBack.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    /**
     * Launches the MeetingRoomActivity and passes the selected focus duration
     * via an intent extra to configure the study timer.
     */
    private void launchMeetingRoom() {
        Intent intent = new Intent(requireContext(), MeetingRoomActivity.class);
        intent.putExtra(MeetingRoomActivity.EXTRA_FOCUS_DURATION, selectedDurationMinutes);
        startActivity(intent);
    }
}