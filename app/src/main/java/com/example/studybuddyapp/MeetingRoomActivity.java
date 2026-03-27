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

/**
 * Hosts the participant video call, focus timer, and session-completion flow for a study session.
 */
public class MeetingRoomActivity extends AppCompatActivity
        implements MeetingRoomVideoCallManager.Callbacks, MeetingRoomSessionCoordinator.Callbacks {

    private static final int PERMISSION_REQ_ID = 100;

    public static final String EXTRA_FOCUS_DURATION = "extra_focus_duration";
    public static final String EXTRA_CHANNEL_NAME = "extra_channel_name";

    // These values describe the room that was chosen before the activity opened.
    private String channelName;
    private int focusDurationMinutes;
    private long focusDurationMs;
    private long currentTimerMs;
    // Main video, thumbnail strip, and timer/speaker labels are rebound after configuration changes.
    private FrameLayout mainVideoContainer;
    private LinearLayout thumbnailContainer;
    private TextView tvTimer, tvSpeakerId;
    // Call controls are tracked explicitly so their icon state can survive layout recreation.
    private ImageView btnSpeaker, btnCamera, btnHangUp, btnMic, btnFlag;
    private boolean isMicMuted = false, isCameraOff = false, isSpeakerOff = false;
    // These flags distinguish a completed session from a temporary background/child-screen transition.
    private boolean isSessionCompleted = false, userLeftForeground = false, isNavigatingToChild = false;
    // Timer and helper classes coordinate the UI countdown, Agora room, and backend session state.
    private CountDownTimer focusTimer;
    private MeetingRoomVideoCallManager videoCallManager;
    private MeetingRoomSessionCoordinator sessionCoordinator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Keep the screen awake while a focus session is active.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_meeting_room);
        // Initialise the room in a predictable order before requesting permissions or joining Agora.
        parseIntentExtras();
        bindViews();
        createManagers();
        wireControlButtons();
        setupBackHandler();
        if (checkPermissions()) {
            // Join immediately when runtime permissions are already available.
            initAndJoin();
        } else {
            // Otherwise defer the join until the permission callback returns.
            requestPermissions();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Only treat this as a distraction when the user actually leaves an active session screen.
        if (videoCallManager.isInChannel() && !isSessionCompleted
                && !isFinishing() && !isNavigatingToChild) {
            userLeftForeground = true;
        }
        // Child screens such as flagging should not trigger the distraction warning on return.
        isNavigatingToChild = false;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // Returning from the background during an unfinished session shows the distraction warning once.
        if (userLeftForeground && videoCallManager.isInChannel() && !isSessionCompleted) {
            userLeftForeground = false;
            showDistractionWarningDialog();
        }
    }

    @Override
    protected void onDestroy() {
        // Tear down timers and backend/Agora state even when the activity is destroyed unexpectedly.
        cancelFocusTimer();
        leaveMeeting();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Rebind all views after a configuration change so video containers point at the new layout.
        setContentView(R.layout.activity_meeting_room);
        bindViews();
        videoCallManager.rebindContainers(mainVideoContainer, thumbnailContainer);
        wireControlButtons();
        refreshUiState();
    }

    @Override
    public void onChannelJoined() {
        // The focus timer only starts after Agora confirms the user actually joined the room.
        startFocusTimer();
        sessionCoordinator.onChannelJoined();
    }

    @Override
    public void onMainViewChanged(int uid) {
        if (tvSpeakerId == null) return;
        if (uid == 0) {
            // The local user is represented as "You" and does not need a visible id label.
            tvSpeakerId.setText("You");
            tvSpeakerId.setVisibility(View.GONE);
        } else {
            // Remote users keep their uid visible when they are promoted to the main view.
            tvSpeakerId.setText("ID: " + uid);
            tvSpeakerId.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean shouldHandleCallbacks() {
        // Coordinator callbacks are ignored once the activity is on its way out.
        return !isFinishing() && !isDestroyed();
    }

    @Override
    public void onUserRestricted(UserProfileResponse profile) {
        // SessionCoordinator uses this callback when polling detects a ban or temporary restriction.
        showRemovalNotification(profile);
    }

    /**
     * Reads the duration and channel values used to configure the meeting room.
     */
    private void parseIntentExtras() {
        focusDurationMinutes = getIntent().getIntExtra(EXTRA_FOCUS_DURATION, 15);
        focusDurationMs = focusDurationMinutes * 60L * 1000L;
        // currentTimerMs starts at the full duration and is updated on each timer tick.
        currentTimerMs = focusDurationMs;
        channelName = getIntent().getStringExtra(EXTRA_CHANNEL_NAME);
        // Fall back to the deterministic channel name helper when the matching backend did not supply one.
        if (channelName == null || channelName.isEmpty()) channelName = AgoraConfig.channelNameForDuration(focusDurationMinutes);
    }

    /**
     * Binds the main call controls and timer views from the current layout.
     */
    private void bindViews() {
        // All view references are rebound from the current layout instance after each recreate.
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

    /**
     * Creates the helper objects that manage Agora state and backend session tracking.
     */
    private void createManagers() {
        // The video manager owns Agora rendering while the session coordinator talks to backend services.
        videoCallManager = new MeetingRoomVideoCallManager(this, mainVideoContainer, thumbnailContainer, this);
        sessionCoordinator = new MeetingRoomSessionCoordinator(this, channelName, focusDurationMinutes, this);
    }

    /**
     * Wires the in-room controls for leaving, muting, camera toggling, and reporting participants.
     */
    private void wireControlButtons() {
        // Hang up always routes through the warning dialog so incomplete sessions are recorded consistently.
        btnHangUp.setOnClickListener(v -> showManualExitWarningDialog());
        btnMic.setOnClickListener(v -> {
            // Keep the local mute state and icon aligned with the Agora audio track state.
            isMicMuted = !isMicMuted;
            videoCallManager.setMicMuted(isMicMuted);
            btnMic.setImageResource(isMicMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic);
        });
        btnCamera.setOnClickListener(v -> {
            // Muting local video also updates the button icon immediately for feedback.
            isCameraOff = !isCameraOff;
            videoCallManager.setCameraOff(isCameraOff);
            btnCamera.setImageResource(isCameraOff ? R.drawable.ic_videocam_off : R.drawable.ic_videocam);
        });
        btnSpeaker.setOnClickListener(v -> {
            // Speaker state is tracked locally so it can be restored after configuration changes.
            isSpeakerOff = !isSpeakerOff;
            videoCallManager.setSpeakerOff(isSpeakerOff);
            btnSpeaker.setImageResource(isSpeakerOff ? R.drawable.ic_speaker_off : R.drawable.ic_speaker);
        });
        btnFlag.setOnClickListener(v -> {
            // Flagging opens a child screen with a snapshot of the current remote participants.
            isNavigatingToChild = true;
            Intent intent = new Intent(this, FlagParticipantActivity.class);
            intent.putExtra(EXTRA_CHANNEL_NAME, channelName);
            intent.putIntegerArrayListExtra("remote_uids", videoCallManager.getRemoteUidsSnapshot());
            startActivity(intent);
        });
    }

    /**
     * Intercepts the system back action while an active session is still in progress.
     */
    private void setupBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Leaving early during an active session should always go through the warning modal.
                if (videoCallManager.isInChannel() && !isSessionCompleted) {
                    showManualExitWarningDialog();
                } else {
                    // Once the session is over, fall back to the normal back-stack behaviour.
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    /**
     * Returns the runtime permissions required for the current Android version.
     */
    private String[] getRequiredPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Android 12+ requires the additional Bluetooth permission for audio routing.
            return new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
        }
        return new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
    }

    /**
     * Checks whether the call permissions have already been granted.
     */
    private boolean checkPermissions() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    /**
     * Requests the missing runtime permissions needed to start the call.
     */
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
            // The room cannot function without microphone and camera access.
            Toast.makeText(this, "Camera and microphone permissions are required", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Initialises Agora with the current user id and joins the chosen meeting room.
     */
    private void initAndJoin() {
        SessionManager session = new SessionManager(this);
        // Logged-in users reuse their backend id as the Agora uid when possible.
        int agoraUid = session.isLoggedIn() && session.getUserId() > 0 ? (int) session.getUserId() : 0;
        // Abort the screen if Agora could not be initialised successfully.
        if (!videoCallManager.initAndJoin(channelName, agoraUid)) finish();
    }

    /**
     * Starts or restarts the countdown shown during the focus session.
     */
    private void startFocusTimer() {
        updateTimerText(focusDurationMs);
        focusTimer = new CountDownTimer(focusDurationMs, 1000) {
            @Override
            public void onTick(long millisLeft) {
                // Keep the timer label and resumable state aligned on every tick.
                updateTimerText(millisLeft);
            }

            @Override
            public void onFinish() {
                isSessionCompleted = true;
                updateTimerText(0);
                // Completing the full timer marks the session as finished before the next dialog appears.
                sessionCoordinator.recordSessionComplete();
                showSessionCompletedDialog();
            }
        }.start();
    }

    /**
     * Stops the countdown if one is currently running.
     */
    private void cancelFocusTimer() {
        if (focusTimer != null) {
            focusTimer.cancel();
            focusTimer = null;
        }
    }

    /**
     * Formats the remaining milliseconds into the timer label used by the UI.
     */
    private void updateTimerText(long millisLeft) {
        currentTimerMs = millisLeft;
        int totalSeconds = (int) (millisLeft / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        // The line break matches the timer design used by the meeting-room layout.
        tvTimer.setText(String.format(Locale.US, "%02d:%02d\nLeft", minutes, seconds));
    }

    /**
     * Reapplies the stored control states after the layout has been recreated.
     */
    private void refreshUiState() {
        // Repaint all toggle icons first so the recreated layout reflects the saved state immediately.
        updateTimerText(currentTimerMs);
        btnMic.setImageResource(isMicMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic);
        btnCamera.setImageResource(isCameraOff ? R.drawable.ic_videocam_off : R.drawable.ic_videocam);
        btnSpeaker.setImageResource(isSpeakerOff ? R.drawable.ic_speaker_off : R.drawable.ic_speaker);
        // Then reapply the same states to Agora so engine state and UI state stay in sync.
        videoCallManager.setMicMuted(isMicMuted);
        videoCallManager.setCameraOff(isCameraOff);
        videoCallManager.setSpeakerOff(isSpeakerOff);
    }

    /**
     * Shows the completion dialog after the timer finishes.
     */
    private void showSessionCompletedDialog() {
        if (isFinishing() || isDestroyed()) return;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_session_completed, null);
        AlertDialog dialog = createDialog(view, false);
        view.findViewById(R.id.btnHome).setOnClickListener(v -> {
            dialog.dismiss();
            // Returning home ends the current room without starting another timer.
            leaveAndGoHome(false);
        });
        view.findViewById(R.id.btnStartNewSession).setOnClickListener(v -> {
            dialog.dismiss();
            // Starting again restarts both the frontend timer and backend session record.
            restartFocusTimer();
        });
        view.findViewById(R.id.btnGoToTaskList).setOnClickListener(v -> {
            dialog.dismiss();
            // The task-list shortcut reuses the existing MainHubActivity entry point.
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

    /**
     * Builds the shared exit-warning dialog used for manual exits and distraction warnings.
     */
    private void showExitDialog(int messageResId, boolean cancelable) {
        if (isFinishing() || isDestroyed()) return;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_exit_warning, null);
        ((TextView) view.findViewById(R.id.tvWarningMessage)).setText(messageResId);
        AlertDialog dialog = createDialog(view, cancelable);
        // Continuing simply closes the warning and leaves the current room untouched.
        view.findViewById(R.id.btnContinueStudying).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnLeaveSession).setOnClickListener(v -> {
            dialog.dismiss();
            // Leaving early records the session as incomplete before returning home.
            if (!isSessionCompleted) sessionCoordinator.recordSessionIncomplete();
            leaveAndGoHome(false);
        });
        dialog.show();
    }

    /**
     * Shows the forced-removal dialog when the user is kicked or restricted during a session.
     */
    private void showRemovalNotification(UserProfileResponse profile) {
        if (isFinishing() || isDestroyed()) return;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_user_removed, null);
        ((TextView) view.findViewById(R.id.tvRemovedTitle)).setText("You Have Been Removed");
        ((TextView) view.findViewById(R.id.tvRemovedMessage)).setText(getRemovalMessage(profile));
        AlertDialog dialog = createDialog(view, false);
        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            dialog.dismiss();
            // Forced removals also count as incomplete sessions unless the timer had already finished.
            if (!isSessionCompleted) sessionCoordinator.recordSessionIncomplete();
            leaveAndGoHome(false);
        });
        dialog.show();
    }

    /**
     * Builds the removal message based on whether the user was kicked or restricted.
     */
    private String getRemovalMessage(UserProfileResponse profile) {
        if (profile.isBanned()) {
            // Permanent moderation actions use the strongest wording.
            return "Your Account Is Banned Permanently, Please Refrain From Conducting Inappropriate Behavior E.G. Nudity, Noise, Violence";
        }
        if (profile.getBannedUntil() != null && isLongTermBan(profile.getBannedUntil())) {
            // Longer temporary bans are distinguished from simple room kicks.
            return "Your Account Is Banned For 3 Days, Please Refrain From Conducting Inappropriate Behavior E.G. Nudity, Noise, Violence";
        }
        // Short restrictions or manual admin removal fall back to the kick message.
        return "You Have Been Kicked Out Of This Meeting By The Administrator.";
    }

    /**
     * Treats bans that still end well in the future as long-term restrictions.
     */
    private boolean isLongTermBan(String bannedUntil) {
        if (bannedUntil == null) return false;
        try {
            java.time.LocalDateTime until = java.time.LocalDateTime.parse(bannedUntil);
            // Very short bans are treated as kicks; longer bans change the copy shown to the user.
            return until.isAfter(java.time.LocalDateTime.now().plusMinutes(1));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Applies the shared rounded-background dialog styling used across this screen.
     */
    private AlertDialog createDialog(View view, boolean cancelable) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(cancelable)
                .create();
        // Dialog layouts provide their own styling, so the default window background is removed.
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        return dialog;
    }

    /**
     * Leaves the current meeting and returns to the main hub, optionally opening the tasks tab.
     */
    private void leaveAndGoHome(boolean goToTasks) {
        cancelFocusTimer();
        leaveMeeting();
        Intent intent = new Intent(this, MainHubActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // The main hub reads this flag to open the tasks tab directly after the room closes.
        if (goToTasks) intent.putExtra("navigate_to_tasks", true);
        startActivity(intent);
        finish();
    }

    /**
     * Starts a fresh timer and backend session after the user chooses another focus round.
     */
    private void restartFocusTimer() {
        isSessionCompleted = false;
        cancelFocusTimer();
        sessionCoordinator.restartSession();
        startFocusTimer();
    }

    /**
     * Ends both the backend session tracking and the Agora call state.
     */
    private void leaveMeeting() {
        sessionCoordinator.onMeetingEnded();
        videoCallManager.leaveAndCleanup();
    }
}
