package com.example.studybuddyapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MeetingRoomActivity extends AppCompatActivity {

    private boolean isVolumePanelVisible = false;
    private boolean isCameraOff = false;
    private boolean isMicOff = false;

    private ImageView btnSpeaker;
    private ImageView btnCamera;
    private ImageView btnHangUp;
    private ImageView btnMic;
    private ImageView btnFlag;

    private LinearLayout volumePanel;
    private SeekBar seekBarVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_meeting_room);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupListeners();

        // Change the delay if you want more time for testing.
        new Handler(Looper.getMainLooper()).postDelayed(this::showSessionCompletedDialog, 30000);
    }

    private void initViews() {
        btnSpeaker = findViewById(R.id.btnSpeaker);
        btnCamera = findViewById(R.id.btnCamera);
        btnHangUp = findViewById(R.id.btnHangUp);
        btnMic = findViewById(R.id.btnMic);
        btnFlag = findViewById(R.id.btnFlag);

        volumePanel = findViewById(R.id.volumePanel);
        seekBarVolume = findViewById(R.id.seekBarVolume);
    }

    private void setupListeners() {
        btnHangUp.setOnClickListener(v -> showManualExitWarningDialog());

        btnFlag.setOnClickListener(v ->
                startActivity(new Intent(this, FlagParticipantActivity.class)));

        btnSpeaker.setOnClickListener(v -> toggleVolumePanel());

        btnCamera.setOnClickListener(v -> {
            isCameraOff = !isCameraOff;
            updateToggleButton(btnCamera, isCameraOff);

        });

        btnMic.setOnClickListener(v -> {
            isMicOff = !isMicOff;
            updateToggleButton(btnMic, isMicOff);
        });

        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Placeholder for future real audio control.
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No action needed.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No action needed.
            }
        });
    }

    private void toggleVolumePanel() {
        isVolumePanelVisible = !isVolumePanelVisible;
        volumePanel.setVisibility(isVolumePanelVisible ? View.VISIBLE : View.GONE);
    }

    private void updateToggleButton(ImageView button, boolean disabled) {
        if (disabled) {
            button.setColorFilter(ContextCompat.getColor(this, R.color.red_hangup));
            button.setAlpha(0.7f);
        } else {
            button.setColorFilter(ContextCompat.getColor(this, android.R.color.white));
            button.setAlpha(1.0f);
        }
    }

    private void showManualExitWarningDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Meeting Room")
                .setMessage("Ending your focus session will affect your rating.")
                .setPositiveButton("Continue Studying", (d, w) -> d.dismiss())
                .setNegativeButton("Leave Session", (d, w) -> finish())
                .setCancelable(true)
                .create();
        dialog.show();
    }

    private void showSessionCompletedDialog() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Session Completed")
                .setMessage("Congratulations! Your focus session is complete.")
                .setCancelable(false)
                .setPositiveButton("Home", (d, w) -> {
                    Intent intent = new Intent(this, MainHubActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Start New Session", (d, w) -> d.dismiss())
                .setNeutralButton("Go To Task List", (d, w) -> {
                    Intent intent = new Intent(this, MainHubActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("navigate_to_tasks", true);
                    startActivity(intent);
                })
                .create();

        dialog.show();
    }
}