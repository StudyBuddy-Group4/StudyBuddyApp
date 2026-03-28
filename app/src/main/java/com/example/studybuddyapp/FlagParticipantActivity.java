package com.example.studybuddyapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
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

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Lets the user report another participant from the current meeting.
 */
public class FlagParticipantActivity extends AppCompatActivity {

    // Current room id
    private String currentMeetingId;
    // Selected reported user
    private long selectedReportedUserId = -1;
    // Logged-in user session
    private SessionManager sessionManager;

    // Participant card views
    private final int[] slotIds = {R.id.slot1, R.id.slot2, R.id.slot3, R.id.slot4};
    // Labels for remote user ids
    private final int[] uidLabelIds = {R.id.tvUid1, R.id.tvUid2, R.id.tvUid3, R.id.tvUid4};
    // Flag buttons for each slot
    private final int[] flagBtnIds = {R.id.flagBtn1, R.id.flagBtn2, R.id.flagBtn3, R.id.flagBtn4};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_flag_participant);

        // Read the current session
        sessionManager = new SessionManager(this);

        // Apply system-bar padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Back closes this screen
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        // Room id for the report request
        currentMeetingId = getIntent().getStringExtra(MeetingRoomActivity.EXTRA_CHANNEL_NAME);
        // Snapshot of current remote users
        List<Integer> remoteUids = getIntent().getIntegerArrayListExtra("remote_uids");

        // Treat a missing list as empty
        if (remoteUids == null) remoteUids = new ArrayList<>();
        setupParticipantSlots(remoteUids);
    }

    /**
     * Fills the participant slots from the current remote user list.
     */
    private void setupParticipantSlots(List<Integer> remoteUids) {
        TextView tvNoParticipants = findViewById(R.id.tvNoParticipants);

        if (remoteUids.isEmpty()) {
            // Show the empty state when nobody can be reported
            tvNoParticipants.setVisibility(View.VISIBLE);
            findViewById(R.id.participantGrid).setVisibility(View.GONE);
            return;
        }

        // Show the participant grid
        tvNoParticipants.setVisibility(View.GONE);
        findViewById(R.id.participantGrid).setVisibility(View.VISIBLE);

        // Only four slots exist in this layout
        int count = Math.min(remoteUids.size(), 4);
        for (int i = 0; i < count; i++) {
            int uid = remoteUids.get(i);

            // Reveal the matching slot
            FrameLayout slot = findViewById(slotIds[i]);
            slot.setVisibility(View.VISIBLE);

            // Show the remote uid
            TextView uidLabel = findViewById(uidLabelIds[i]);
            uidLabel.setText("User ID: " + uid);

            findViewById(flagBtnIds[i]).setOnClickListener(v -> {
                // Remember which user is being reported
                selectedReportedUserId = uid;
                showFlagReasonDialog();
            });
        }

        // Show the second row only when needed
        findViewById(R.id.row2).setVisibility(count > 2 ? View.VISIBLE : View.GONE);
    }

    /**
     * Opens the reason dialog before sending a report.
     */
    private void showFlagReasonDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_flag_reason, null);
        // Reason input
        EditText etReason = dialogView.findViewById(R.id.etReason);

        // Use the custom flag dialog layout
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            // The layout already provides its own styling
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            // Read the typed reason
            String reasonText = etReason.getText().toString().trim();
            if (reasonText.isEmpty()) {
                // A report needs a written reason
                Toast.makeText(this, "Please provide a reason.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Submit and close the dialog
            submitReportToBackend(reasonText);
            dialog.dismiss();
        });

        // Cancel keeps the user on the same screen
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * Sends the report to the backend.
     */
    private void submitReportToBackend(String reason) {
        // Reporter id from the current session
        long currentUserId = sessionManager.getUserId();
        // Client-side timestamp
        long timestamp = System.currentTimeMillis();

        // Build the report body
        ReportRequest request = new ReportRequest(
                currentUserId, selectedReportedUserId, currentMeetingId, reason, timestamp);

        // Submit through the moderation API
        ModerationApi api = ApiClient.getModerationApi(this);
        api.submitReport(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Close the screen after a successful report
                    Toast.makeText(FlagParticipantActivity.this,
                            "Flag submitted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    // Stay on the page when the backend rejects the request
                    Toast.makeText(FlagParticipantActivity.this,
                            "Failed to submit report.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Network errors leave the current screen open
                Toast.makeText(FlagParticipantActivity.this,
                        "Network error.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
