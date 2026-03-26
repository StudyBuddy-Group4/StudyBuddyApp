package com.example.studybuddyapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.studybuddyapp.api.dto.UserProfileResponse;

import java.util.Locale;

public class MeetingRoomActivity extends AppCompatActivity
        implements MeetingRoomVideoCallManager.Callbacks, MeetingRoomSessionCoordinator.Callbacks {

    private static final int PERMISSION_REQ_ID = 100;

    public static final String EXTRA_FOCUS_DURATION = "extra_focus_duration";
    public static final String EXTRA_CHANNEL_NAME = "extra_channel_name";

    private String channelName;
    private int focusDurationMinutes;
    private long focusDurationMs;
    private long currentTimerMs;
    private FrameLayout mainVideoContainer;
    private LinearLayout thumbnailContainer;
    private TextView tvTimer, tvSpeakerId;
    private ImageView btnSpeaker, btnCamera, btnHangUp, btnMic, btnFlag;
    private boolean isMicMuted = false, isCameraOff = false, isSpeakerOff = false;
    private boolean isSessionCompleted = false, userLeftForeground = false, isNavigatingToChild = false;
    private CountDownTimer focusTimer;
    private MeetingRoomVideoCallManager videoCallManager;
    private MeetingRoomSessionCoordinator sessionCoordinator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_meeting_room);
        parseIntentExtras();
        bindViews();
        createManagers();
        wireControlButtons();
        setupBackHandler();
        if (checkPermissions()) {
            initAndJoin();
        } else {
            requestPermissions();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (videoCallManager.isInChannel() && !isSessionCompleted
                && !isFinishing() && !isNavigatingToChild) {
            userLeftForeground = true;
        }
        isNavigatingToChild = false;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (userLeftForeground && videoCallManager.isInChannel() && !isSessionCompleted) {
            userLeftForeground = false;
            showDistractionWarningDialog();
        }
    }

    @Override
    protected void onDestroy() {
        cancelFocusTimer();
        leaveMeeting();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_meeting_room);
        bindViews();
        videoCallManager.rebindContainers(mainVideoContainer, thumbnailContainer);
        wireControlButtons();
        refreshUiState();
    }

    @Override
    public void onChannelJoined() {
        startFocusTimer();
        sessionCoordinator.onChannelJoined();
    }

    @Override
    public void onMainViewChanged(int uid) {
        if (tvSpeakerId == null) return;
        if (uid == 0) {
            tvSpeakerId.setText("You");
            tvSpeakerId.setVisibility(View.GONE);
        } else {
            tvSpeakerId.setText("ID: " + uid);
            tvSpeakerId.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean shouldHandleCallbacks() {
        return !isFinishing() && !isDestroyed();
    }

    @Override
    public void onUserRestricted(UserProfileResponse profile) {
        showRemovalNotification(profile);
    }

    private void parseIntentExtras() {
        focusDurationMinutes = getIntent().getIntExtra(EXTRA_FOCUS_DURATION, 15);
        focusDurationMs = focusDurationMinutes * 60L * 1000L;
        currentTimerMs = focusDurationMs;
        channelName = getIntent().getStringExtra(EXTRA_CHANNEL_NAME);
        if (channelName == null || channelName.isEmpty()) channelName = AgoraConfig.channelNameForDuration(focusDurationMinutes);
    }

    private void bindViews() {
        mainVideoContainer = findViewById(R.id.mainVideoContainer);
        thumbnailContainer = findViewById(R.id.thumbnailContainer);
        tvTimer = findViewById(R.id.tvTimer);
        tvSpeakerId = findViewById(R.id.tvSpeakerId);
        btnSpeaker = findViewById(R.id.btnSpeaker);
        btnCamera = findViewById(R.id.btnCamera);
        btnHangUp = findViewById(R.id.btnHangUp);
        btnMic = findViewById(R.id.btnMic);
        btnFlag = findViewById(R.id.btnFlag);
    }

    private void createManagers() {
        videoCallManager = new MeetingRoomVideoCallManager(this, mainVideoContainer, thumbnailContainer, this);
        sessionCoordinator = new MeetingRoomSessionCoordinator(this, channelName, focusDurationMinutes, this);
    }

    private void wireControlButtons() {
        btnHangUp.setOnClickListener(v -> showManualExitWarningDialog());
        btnMic.setOnClickListener(v -> {
            isMicMuted = !isMicMuted;
            videoCallManager.setMicMuted(isMicMuted);
            btnMic.setImageResource(isMicMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic);
        });
        btnCamera.setOnClickListener(v -> {
            isCameraOff = !isCameraOff;
            videoCallManager.setCameraOff(isCameraOff);
            btnCamera.setImageResource(isCameraOff ? R.drawable.ic_videocam_off : R.drawable.ic_videocam);
        });
        btnSpeaker.setOnClickListener(v -> {
            isSpeakerOff = !isSpeakerOff;
            videoCallManager.setSpeakerOff(isSpeakerOff);
            btnSpeaker.setImageResource(isSpeakerOff ? R.drawable.ic_speaker_off : R.drawable.ic_speaker);
        });
        btnFlag.setOnClickListener(v -> {
            isNavigatingToChild = true;
            Intent intent = new Intent(this, FlagParticipantActivity.class);
            intent.putExtra(EXTRA_CHANNEL_NAME, channelName);
            intent.putIntegerArrayListExtra("remote_uids", videoCallManager.getRemoteUidsSnapshot());
            startActivity(intent);
        });
    }

    private void setupBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (videoCallManager.isInChannel() && !isSessionCompleted) {
                    showManualExitWarningDialog();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private String[] getRequiredPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
        }
        return new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
    }

    private boolean checkPermissions() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, getRequiredPermissions(), PERMISSION_REQ_ID);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != PERMISSION_REQ_ID) return;
        if (checkPermissions()) {
            initAndJoin();
        } else {
            Toast.makeText(this, "Camera and microphone permissions are required", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initAndJoin() {
        SessionManager session = new SessionManager(this);
        int agoraUid = session.isLoggedIn() && session.getUserId() > 0 ? (int) session.getUserId() : 0;
        if (!videoCallManager.initAndJoin(channelName, agoraUid)) finish();
    }

    private void startFocusTimer() {
        updateTimerText(focusDurationMs);
        focusTimer = new CountDownTimer(focusDurationMs, 1000) {
            @Override
            public void onTick(long millisLeft) {
                updateTimerText(millisLeft);
            }

            @Override
            public void onFinish() {
                isSessionCompleted = true;
                updateTimerText(0);
                sessionCoordinator.recordSessionComplete();
                showSessionCompletedDialog();
            }
        }.start();
    }

    private void cancelFocusTimer() {
        if (focusTimer != null) {
            focusTimer.cancel();
            focusTimer = null;
        }
    }

    private void updateTimerText(long millisLeft) {
        currentTimerMs = millisLeft;
        int totalSeconds = (int) (millisLeft / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        tvTimer.setText(String.format(Locale.US, "%02d:%02d\nLeft", minutes, seconds));
    }

    private void refreshUiState() {
        updateTimerText(currentTimerMs);
        btnMic.setImageResource(isMicMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic);
        btnCamera.setImageResource(isCameraOff ? R.drawable.ic_videocam_off : R.drawable.ic_videocam);
        btnSpeaker.setImageResource(isSpeakerOff ? R.drawable.ic_speaker_off : R.drawable.ic_speaker);
        videoCallManager.setMicMuted(isMicMuted);
        videoCallManager.setCameraOff(isCameraOff);
        videoCallManager.setSpeakerOff(isSpeakerOff);
    }

    private void showSessionCompletedDialog() {
        if (isFinishing() || isDestroyed()) return;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_session_completed, null);
        AlertDialog dialog = createDialog(view, false);
        view.findViewById(R.id.btnHome).setOnClickListener(v -> {
            dialog.dismiss();
            leaveAndGoHome(false);
        });
        view.findViewById(R.id.btnStartNewSession).setOnClickListener(v -> {
            dialog.dismiss();
            restartFocusTimer();
        });
        view.findViewById(R.id.btnGoToTaskList).setOnClickListener(v -> {
            dialog.dismiss();
            leaveAndGoHome(true);
        });
        dialog.show();
    }

    private void showManualExitWarningDialog() {
        showExitDialog(R.string.manual_exit_warning, true);
    }

    private void showDistractionWarningDialog() {
        showExitDialog(R.string.distraction_warning, false);
    }

    private void showExitDialog(int messageResId, boolean cancelable) {
        if (isFinishing() || isDestroyed()) return;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_exit_warning, null);
        ((TextView) view.findViewById(R.id.tvWarningMessage)).setText(messageResId);
        AlertDialog dialog = createDialog(view, cancelable);
        view.findViewById(R.id.btnContinueStudying).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnLeaveSession).setOnClickListener(v -> {
            dialog.dismiss();
            if (!isSessionCompleted) sessionCoordinator.recordSessionIncomplete();
            leaveAndGoHome(false);
        });
        dialog.show();
    }

    private void showRemovalNotification(UserProfileResponse profile) {
        if (isFinishing() || isDestroyed()) return;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_user_removed, null);
        ((TextView) view.findViewById(R.id.tvRemovedTitle)).setText("You Have Been Removed");
        ((TextView) view.findViewById(R.id.tvRemovedMessage)).setText(getRemovalMessage(profile));
        AlertDialog dialog = createDialog(view, false);
        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            dialog.dismiss();
            if (!isSessionCompleted) sessionCoordinator.recordSessionIncomplete();
            leaveAndGoHome(false);
        });
        dialog.show();
    }

    private String getRemovalMessage(UserProfileResponse profile) {
        if (profile.isBanned()) {
            return "Your Account Is Banned Permanently, Please Refrain From Conducting Inappropriate Behavior E.G. Nudity, Noise, Violence";
        }
        if (profile.getBannedUntil() != null && isLongTermBan(profile.getBannedUntil())) {
            return "Your Account Is Banned For 3 Days, Please Refrain From Conducting Inappropriate Behavior E.G. Nudity, Noise, Violence";
        }
        return "You Have Been Kicked Out Of This Meeting By The Administrator.";
    }

    private boolean isLongTermBan(String bannedUntil) {
        if (bannedUntil == null) return false;
        try {
            java.time.LocalDateTime until = java.time.LocalDateTime.parse(bannedUntil);
            return until.isAfter(java.time.LocalDateTime.now().plusMinutes(1));
        } catch (Exception e) {
            return false;
        }
    }

    private AlertDialog createDialog(View view, boolean cancelable) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(cancelable)
                .create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        return dialog;
    }

    private void leaveAndGoHome(boolean goToTasks) {
        cancelFocusTimer();
        leaveMeeting();
        Intent intent = new Intent(this, MainHubActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (goToTasks) intent.putExtra("navigate_to_tasks", true);
        startActivity(intent);
        finish();
    }

    private void restartFocusTimer() {
        isSessionCompleted = false;
        cancelFocusTimer();
        sessionCoordinator.restartSession();
        startFocusTimer();
    }

    private void leaveMeeting() {
        sessionCoordinator.onMeetingEnded();
        videoCallManager.leaveAndCleanup();
    }
}
