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

public class FlagParticipantActivity extends AppCompatActivity {

    private String currentMeetingId;
    private long selectedReportedUserId = -1;
    private SessionManager sessionManager;

    private final int[] slotIds = {R.id.slot1, R.id.slot2, R.id.slot3, R.id.slot4};
    private final int[] uidLabelIds = {R.id.tvUid1, R.id.tvUid2, R.id.tvUid3, R.id.tvUid4};
    private final int[] flagBtnIds = {R.id.flagBtn1, R.id.flagBtn2, R.id.flagBtn3, R.id.flagBtn4};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_flag_participant);

        sessionManager = new SessionManager(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        currentMeetingId = getIntent().getStringExtra(MeetingRoomActivity.EXTRA_CHANNEL_NAME);
        List<Integer> remoteUids = getIntent().getIntegerArrayListExtra("remote_uids");

        if (remoteUids == null) remoteUids = new ArrayList<>();
        setupParticipantSlots(remoteUids);
    }

    private void setupParticipantSlots(List<Integer> remoteUids) {
        TextView tvNoParticipants = findViewById(R.id.tvNoParticipants);

        if (remoteUids.isEmpty()) {
            tvNoParticipants.setVisibility(View.VISIBLE);
            findViewById(R.id.participantGrid).setVisibility(View.GONE);
            return;
        }

        tvNoParticipants.setVisibility(View.GONE);
        findViewById(R.id.participantGrid).setVisibility(View.VISIBLE);

        int count = Math.min(remoteUids.size(), 4);
        for (int i = 0; i < count; i++) {
            int uid = remoteUids.get(i);

            FrameLayout slot = findViewById(slotIds[i]);
            slot.setVisibility(View.VISIBLE);

            TextView uidLabel = findViewById(uidLabelIds[i]);
            uidLabel.setText("User ID: " + uid);

            findViewById(flagBtnIds[i]).setOnClickListener(v -> {
                selectedReportedUserId = uid;
                showFlagReasonDialog();
            });
        }

        findViewById(R.id.row2).setVisibility(count > 2 ? View.VISIBLE : View.GONE);
    }

    private void showFlagReasonDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_flag_reason, null);
        EditText etReason = dialogView.findViewById(R.id.etReason);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

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

    private void submitReportToBackend(String reason) {
        long currentUserId = sessionManager.getUserId();
        long timestamp = System.currentTimeMillis();

        ReportRequest request = new ReportRequest(
                currentUserId, selectedReportedUserId, currentMeetingId, reason, timestamp);

        ModerationApi api = ApiClient.getModerationApi(this);
        api.submitReport(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FlagParticipantActivity.this,
                            "Flag submitted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(FlagParticipantActivity.this,
                            "Failed to submit report.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(FlagParticipantActivity.this,
                        "Network error.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
