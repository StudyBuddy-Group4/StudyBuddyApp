package com.example.studybuddyapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.ModerationApi;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity specifically designed for Administrators to monitor flagged meetings.
 * This class provides a grid view of participants with "prohibited" icons allowing
 * the admin to issue real-time punishments, fulfilling the moderation requirements.
 * * It connects to the backend ModerationApi to enforce rules and maintain a safe
 * study environment for all users.
 */
public class AdminMeetingRoomActivity extends AppCompatActivity {

    // Stores the ID of the user that the admin currently wants to penalize.
    // In a fully dynamic implementation, this would be retrieved from the clicked view's tag.
    private long selectedUserId = -1;

    /**
     * Initializes the activity, sets up the edge-to-edge display, and binds
     * the click listeners to the prohibition buttons.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down then this Bundle contains the data it most
     * recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_meeting_room);
        
        // Apply window insets to avoid overlapping with system bars (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Close the admin view and return to the previous screen
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        // Define a common click listener for all prohibit icons in the grid
        View.OnClickListener prohibitClickListener = v -> {
            // Assign a dummy user ID for demonstration. 
            // In a real app, you would do: selectedUserId = (long) v.getTag();
            selectedUserId = 12345L; 
            showAdminDecisionDialog();
        };
        
        // Attach the listener to the static buttons in the layout
        findViewById(R.id.prohibitBtn1).setOnClickListener(prohibitClickListener);
        findViewById(R.id.prohibitBtn2).setOnClickListener(prohibitClickListener);
        findViewById(R.id.prohibitBtn3).setOnClickListener(prohibitClickListener);
        findViewById(R.id.prohibitBtn4).setOnClickListener(prohibitClickListener);
        findViewById(R.id.prohibitBtn5).setOnClickListener(prohibitClickListener);
    }

    /**
     * Displays the dialog with moderation options for the selected account.
     * This maps the admin's UI choice to a specific backend API action,
     * fulfilling requirements MOD-5, MOD-7, and MOD-8.
     */
    private void showAdminDecisionDialog() {
        // Inflate the custom dialog layout for admin decisions
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_admin_decision, null);

        // Build and configure the AlertDialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Make the dialog background transparent to match the UI design
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Instantly remove user from the meeting [MOD-5]
        dialogView.findViewById(R.id.btnKickOut).setOnClickListener(v -> {
            executeAdminAction("KICK");
            dialog.dismiss();
        });

        // Temporarily ban the user for 3 days [MOD-7]
        dialogView.findViewById(R.id.btnBan3Days).setOnClickListener(v -> {
            executeAdminAction("BAN_3_DAYS");
            dialog.dismiss();
        });

        // Permanently ban the user [MOD-8]
        dialogView.findViewById(R.id.btnBanPermanently).setOnClickListener(v -> {
            executeAdminAction("BAN_PERMANENT");
            dialog.dismiss();
        });

        // Dismiss the dialog without taking any action
        dialogView.findViewById(R.id.btnBack).setOnClickListener(v -> dialog.dismiss());

        // Display the dialog to the admin
        dialog.show();
    }

    /**
     * Executes the chosen administrative action via a network call to the backend.
     * * @param actionType A string constant representing the type of punishment 
     * (e.g., "KICK", "BAN_3_DAYS", "BAN_PERMANENT").
     */
    private void executeAdminAction(String actionType) {
        // Create the Moderation API service using the Retrofit client
        ModerationApi api = ApiClient.getModerationApi(this);
        
        // Enqueue the asynchronous network request
        api.applyAdminAction(selectedUserId, actionType).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // Check if the backend successfully processed the punishment
                if(response.isSuccessful()) {
                    Toast.makeText(AdminMeetingRoomActivity.this, 
                        "Action " + actionType + " applied.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AdminMeetingRoomActivity.this, 
                        "Server rejected the action.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Handle network failures or serialization errors
                Toast.makeText(AdminMeetingRoomActivity.this, 
                    "Failed to apply action due to network error.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}