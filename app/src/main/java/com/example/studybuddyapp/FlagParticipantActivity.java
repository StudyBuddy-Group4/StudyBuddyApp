package com.example.studybuddyapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.ModerationApi;
import com.example.studybuddyapp.api.dto.ReportRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity responsible for allowing a user to flag another participant
 * for inappropriate behavior during an active meeting.
 * This satisfies requirement [MOD-1] by providing a UI for the user
 * to select a participant and submit a written reason.
 */
public class FlagParticipantActivity extends AppCompatActivity {

    // Variables to hold the context passed from the MeetingRoomActivity
    private String currentMeetingId;
    private long selectedReportedUserId = -1; // Default invalid ID
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_flag_participant);
        
        sessionManager = new SessionManager(this);
        
        // Extract the meeting ID passed from the MeetingRoomActivity intent
        if (getIntent() != null) {
            currentMeetingId = getIntent().getStringExtra(MeetingRoomActivity.EXTRA_CHANNEL_NAME);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        // In a real implementation, each button would represent a specific user's ID.
        // For demonstration, we assign a dummy ID when a flag button is clicked.
        View.OnClickListener flagClickListener = v -> {
            selectedReportedUserId = 12345L; // Example: extract actual UID from the clicked view's tag
            showFlagReasonDialog();
        };
        
        findViewById(R.id.flagBtn1).setOnClickListener(flagClickListener);
        findViewById(R.id.flagBtn2).setOnClickListener(flagClickListener);
        // ... wire other buttons ...
    }

    /**
     * Displays a dialog allowing the user to input a written reason for the flag.
     * Upon saving, it triggers the network call to store the report [MOD-3].
     */
    private void showFlagReasonDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_flag_reason, null);
        EditText etReason = dialogView.findViewById(R.id.etReason); // Ensure this ID exists in your XML

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Handle the Save button click event
        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String reasonText = etReason.getText().toString().trim();
            if (reasonText.isEmpty()) {
                Toast.makeText(this, "Please provide a reason.", Toast.LENGTH_SHORT).show();
                return;
            }
            submitReportToBackend(reasonText);
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * Submits the report payload to the backend via Retrofit.
     * * @param reason The written string explaining the inappropriate behavior.
     */
    private void submitReportToBackend(String reason) {
        long currentUserId = sessionManager.getUserId();
        long timestamp = System.currentTimeMillis();
        
        // Create the DTO with all required tracking information
        ReportRequest request = new ReportRequest(currentUserId, selectedReportedUserId, currentMeetingId, reason, timestamp);

        ModerationApi api = ApiClient.getModerationApi(this);
        api.submitReport(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FlagParticipantActivity.this, "Flag submitted successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Return to the meeting room
                } else {
                    Toast.makeText(FlagParticipantActivity.this, "Failed to submit report.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(FlagParticipantActivity.this, "Network error.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}