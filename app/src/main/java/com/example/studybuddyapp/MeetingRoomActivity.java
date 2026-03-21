package com.example.studybuddyapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.MatchingApi;
import com.example.studybuddyapp.api.SessionApi;
import com.example.studybuddyapp.api.TaskApi;
import com.example.studybuddyapp.api.dto.AssignTasksRequest;
import com.example.studybuddyapp.api.dto.LeaveMeetingResponse;
import com.example.studybuddyapp.api.dto.StartSessionRequest;
import com.example.studybuddyapp.api.dto.StartSessionResponse;

import android.os.Handler;
import android.os.Looper;

import com.example.studybuddyapp.api.UserApi;
import com.example.studybuddyapp.api.dto.UserProfileResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeetingRoomActivity extends AppCompatActivity {

    private static final String TAG = "MeetingRoom";
    private static final int PERMISSION_REQ_ID = 100;

    public static final String EXTRA_FOCUS_DURATION = "extra_focus_duration";
    public static final String EXTRA_CHANNEL_NAME = "extra_channel_name";

    private RtcEngine rtcEngine;
    private String channelName;
    private int focusDurationMinutes;

    private FrameLayout mainVideoContainer;
    private LinearLayout thumbnailContainer;
    private TextView tvTimer;
    private ImageView btnSpeaker, btnCamera, btnHangUp, btnMic, btnFlag;

    private boolean isMicMuted = false;
    private boolean isCameraOff = false;
    private boolean isSpeakerOff = false;
    private boolean isSessionCompleted = false;
    private boolean userLeftForeground = false;
    private boolean isInChannel = false;
    private boolean isNavigatingToChild = false;
    private long focusDurationMs;
    private CountDownTimer focusTimer;

    private long backendSessionId = -1;
    private Handler banCheckHandler;
    private static final long BAN_CHECK_INTERVAL_MS = 5000;

    private final List<Integer> remoteUids = new ArrayList<>();
    private final Map<Integer, SurfaceView> remoteSurfaces = new HashMap<>();
    private int mainViewUid = 0;
    private SurfaceView localSurface;

    private final IRtcEngineEventHandler rtcEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            Log.d(TAG, "Joined channel: " + channel + " uid=" + uid);
            runOnUiThread(() -> {
                isInChannel = true;
                startFocusTimer();
                recordSessionStart();
                startBanPolling();
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            Log.d(TAG, "Remote user joined: " + uid);
            runOnUiThread(() -> addRemoteUser(uid));
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            Log.d(TAG, "Remote user offline: " + uid);
            runOnUiThread(() -> removeRemoteUser(uid));
        }

        @Override
        public void onError(int err) {
            Log.e(TAG, "Agora error: " + err);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_meeting_room);

        parseIntentExtras();
        bindViews();
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
        if (isInChannel && !isSessionCompleted && !isFinishing() && !isNavigatingToChild) {
            userLeftForeground = true;
        }
        isNavigatingToChild = false;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (userLeftForeground && isInChannel && !isSessionCompleted) {
            userLeftForeground = false;
            showDistractionWarningDialog();
        }
    }

    @Override
    protected void onDestroy() {
        if (focusTimer != null) focusTimer.cancel();
        stopBanPolling();
        leaveAndCleanup();
        super.onDestroy();
    }

    private void setupBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isInChannel && !isSessionCompleted) {
                    showManualExitWarningDialog();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void parseIntentExtras() {
        focusDurationMinutes = getIntent().getIntExtra(EXTRA_FOCUS_DURATION, 15);
        focusDurationMs = focusDurationMinutes * 60L * 1000L;
        channelName = getIntent().getStringExtra(EXTRA_CHANNEL_NAME);
        if (channelName == null || channelName.isEmpty()) {
            channelName = AgoraConfig.channelNameForDuration(focusDurationMinutes);
        }
    }

    private void bindViews() {
        mainVideoContainer = findViewById(R.id.mainVideoContainer);
        thumbnailContainer = findViewById(R.id.thumbnailContainer);
        tvTimer = findViewById(R.id.tvTimer);
        btnSpeaker = findViewById(R.id.btnSpeaker);
        btnCamera = findViewById(R.id.btnCamera);
        btnHangUp = findViewById(R.id.btnHangUp);
        btnMic = findViewById(R.id.btnMic);
        btnFlag = findViewById(R.id.btnFlag);
    }

    private void wireControlButtons() {
        btnHangUp.setOnClickListener(v -> showManualExitWarningDialog());

        btnMic.setOnClickListener(v -> {
            isMicMuted = !isMicMuted;
            if (rtcEngine != null) rtcEngine.muteLocalAudioStream(isMicMuted);
            btnMic.setImageResource(isMicMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic);
        });

        btnCamera.setOnClickListener(v -> {
            isCameraOff = !isCameraOff;
            if (rtcEngine != null) rtcEngine.muteLocalVideoStream(isCameraOff);
            btnCamera.setImageResource(isCameraOff ? R.drawable.ic_videocam_off : R.drawable.ic_videocam);
            if (localSurface != null) {
                localSurface.setVisibility(isCameraOff ? View.GONE : View.VISIBLE);
            }
        });

        btnSpeaker.setOnClickListener(v -> {
            isSpeakerOff = !isSpeakerOff;
            if (rtcEngine != null) rtcEngine.setEnableSpeakerphone(!isSpeakerOff);
            btnSpeaker.setImageResource(isSpeakerOff ? R.drawable.ic_speaker_off : R.drawable.ic_speaker);
        });

        btnFlag.setOnClickListener(v -> {
            isNavigatingToChild = true;
            Intent intent = new Intent(this, FlagParticipantActivity.class);
            intent.putExtra(EXTRA_CHANNEL_NAME, channelName);
            intent.putIntegerArrayListExtra("remote_uids", new ArrayList<>(remoteUids));
            startActivity(intent);
        });
    }

    // Permissions

    private String[] getRequiredPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
        }
        return new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
        };
    }

    private boolean checkPermissions() {
        for (String perm : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
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
        if (requestCode == PERMISSION_REQ_ID) {
            if (checkPermissions()) {
                initAndJoin();
            } else {
                Toast.makeText(this, "Camera and microphone permissions are required",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    // Agora engine

    private void initAndJoin() {
        if (AgoraConfig.APP_ID.isEmpty()) {
            Toast.makeText(this,
                    "Please set your Agora App ID in AgoraConfig.java",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        initRtcEngine();
        setupLocalPreview();

        SessionManager session = new SessionManager(this);
        int agoraUid = 0;
        if (session.isLoggedIn() && session.getUserId() > 0) {
            agoraUid = (int) session.getUserId();
        }
        joinChannel(agoraUid);
    }

    private void initRtcEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = AgoraConfig.APP_ID;
            config.mEventHandler = rtcEventHandler;
            rtcEngine = RtcEngine.create(config);

            rtcEngine.enableVideo();
            rtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_640x360,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
            ));
            rtcEngine.setEnableSpeakerphone(true);
        } catch (Exception e) {
            Log.e(TAG, "Failed to init RtcEngine", e);
            Toast.makeText(this, "Error initialising video engine", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupLocalPreview() {
        localSurface = new SurfaceView(this);
        mainVideoContainer.addView(localSurface, 0,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
        rtcEngine.setupLocalVideo(new VideoCanvas(localSurface, VideoCanvas.RENDER_MODE_HIDDEN, 0));
        rtcEngine.startPreview();
        mainViewUid = 0;
    }

    private void joinChannel(int uid) {
        String token = AgoraConfig.TEMP_TOKEN.isEmpty() ? null : AgoraConfig.TEMP_TOKEN;

        ChannelMediaOptions options = new ChannelMediaOptions();
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        options.publishCameraTrack = true;
        options.publishMicrophoneTrack = true;
        options.autoSubscribeAudio = true;
        options.autoSubscribeVideo = true;

        int result = rtcEngine.joinChannel(token, channelName, uid, options);
        if (result != 0) {
            Log.e(TAG, "joinChannel failed: " + result);
            Toast.makeText(this, "Failed to join channel (code " + result + ")",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void leaveAndCleanup() {
        isInChannel = false;
        notifyMatchingLeave();
        if (rtcEngine != null) {
            rtcEngine.stopPreview();
            rtcEngine.leaveChannel();
            RtcEngine.destroy();
            rtcEngine = null;
        }
    }

    private void notifyMatchingLeave() {
        if (channelName == null || channelName.isEmpty()) return;
        MatchingApi matchingApi = ApiClient.getMatchingApi(this);
        matchingApi.leaveMeeting(channelName).enqueue(new Callback<LeaveMeetingResponse>() {
            @Override
            public void onResponse(Call<LeaveMeetingResponse> call,
                                   Response<LeaveMeetingResponse> response) {
                Log.d(TAG, "Notified matching engine of leave");
            }

            @Override
            public void onFailure(Call<LeaveMeetingResponse> call, Throwable t) {
                Log.w(TAG, "Failed to notify matching engine of leave", t);
            }
        });
    }

    // Ban polling -- force-remove user if admin bans them mid-meeting

    private void startBanPolling() {
        banCheckHandler = new Handler(Looper.getMainLooper());
        banCheckHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isInChannel || isFinishing() || isDestroyed()) return;
                checkBanStatus();
                banCheckHandler.postDelayed(this, BAN_CHECK_INTERVAL_MS);
            }
        }, BAN_CHECK_INTERVAL_MS);
    }

    private void stopBanPolling() {
        if (banCheckHandler != null) {
            banCheckHandler.removeCallbacksAndMessages(null);
            banCheckHandler = null;
        }
    }

    private void checkBanStatus() {
        UserApi api = ApiClient.getUserApi(this);
        api.getProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call,
                                   Response<UserProfileResponse> response) {
                if (isFinishing() || isDestroyed() || !isInChannel) return;
                if (response.isSuccessful() && response.body() != null) {
                    UserProfileResponse profile = response.body();
                    if (profile.isCurrentlyRestricted()) {
                        stopBanPolling();
                        showRemovalNotification(profile);
                    }
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                // Silently retry next interval
            }
        });
    }

    private void showRemovalNotification(UserProfileResponse profile) {
        if (isFinishing() || isDestroyed()) return;

        String title = "You Have Been Removed";
        String message;
        if (profile.isBanned()) {
            message = "Your Account Is Banned Permanently, Please Refrain From Conducting Inappropriate Behavior E.G. Nudity, Noise, Violence";
        } else if (profile.getBannedUntil() != null && isLongTermBan(profile.getBannedUntil())) {
            message = "Your Account Is Banned For 3 Days, Please Refrain From Conducting Inappropriate Behavior E.G. Nudity, Noise, Violence";
        } else {
            message = "You Have Been Kicked Out Of This Meeting By The Administrator.";
        }

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_user_removed, null);

        ((TextView) view.findViewById(R.id.tvRemovedTitle)).setText(title);
        ((TextView) view.findViewById(R.id.tvRemovedMessage)).setText(message);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            dialog.dismiss();
            if (!isSessionCompleted) recordSessionIncomplete();
            leaveAndGoHome(false);
        });

        dialog.show();
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

    // Backend session recording

    private void recordSessionStart() {
        SessionApi api = ApiClient.getSessionApi(this);
        StartSessionRequest req = new StartSessionRequest(focusDurationMinutes, channelName);
        api.startSession(req).enqueue(new Callback<StartSessionResponse>() {
            @Override
            public void onResponse(Call<StartSessionResponse> call,
                                   Response<StartSessionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    backendSessionId = response.body().getSessionId();
                    Log.d(TAG, "Backend session started: " + backendSessionId);
                    assignPendingTasks(backendSessionId);
                }
            }

            @Override
            public void onFailure(Call<StartSessionResponse> call, Throwable t) {
                Log.w(TAG, "Failed to record session start", t);
            }
        });
    }

    private void assignPendingTasks(long sessionId) {
        TaskApi taskApi = ApiClient.getTaskApi(this);
        taskApi.assignTasksToSession(new AssignTasksRequest(sessionId))
                .enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        Log.d(TAG, "Tasks assigned to session " + sessionId);
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        Log.w(TAG, "Failed to assign tasks", t);
                    }
                });
    }

    private void recordSessionComplete() {
        if (backendSessionId < 0) return;
        SessionApi api = ApiClient.getSessionApi(this);
        api.completeSession(backendSessionId).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.d(TAG, "Session marked complete");
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.w(TAG, "Failed to mark session complete", t);
            }
        });
    }

    private void recordSessionIncomplete() {
        if (backendSessionId < 0) return;
        SessionApi api = ApiClient.getSessionApi(this);
        api.incompleteSession(backendSessionId).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.d(TAG, "Session marked incomplete");
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.w(TAG, "Failed to mark session incomplete", t);
            }
        });
    }

    // Video layout management (Speaker layout)

    private void addRemoteUser(int uid) {
        if (remoteUids.contains(uid)) return;
        remoteUids.add(uid);

        SurfaceView surface = new SurfaceView(this);
        surface.setZOrderMediaOverlay(true);
        remoteSurfaces.put(uid, surface);

        if (mainViewUid == 0 && remoteUids.size() == 1) {
            promoteToMainView(uid);
            moveLocalToThumbnail();
        } else {
            addThumbnailForUid(uid, surface);
        }

        rtcEngine.setupRemoteVideo(new VideoCanvas(surface, VideoCanvas.RENDER_MODE_HIDDEN, uid));
    }

    private void removeRemoteUser(int uid) {
        remoteUids.remove(Integer.valueOf(uid));
        remoteSurfaces.remove(uid);
        removeThumbnailForUid(uid);

        if (mainViewUid == uid) {
            clearMainView();
            if (!remoteUids.isEmpty()) {
                int nextUid = remoteUids.get(0);
                removeThumbnailForUid(nextUid);
                promoteToMainView(nextUid);
            } else {
                moveLocalToMainView();
            }
        }
    }

    private void promoteToMainView(int uid) {
        SurfaceView surface = remoteSurfaces.get(uid);
        if (surface == null) return;
        if (surface.getParent() != null) {
            ((FrameLayout) surface.getParent()).removeView(surface);
        }
        mainVideoContainer.addView(surface, 0,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
        mainViewUid = uid;
    }

    private void clearMainView() {
        for (int i = mainVideoContainer.getChildCount() - 1; i >= 0; i--) {
            View child = mainVideoContainer.getChildAt(i);
            if (child instanceof SurfaceView) {
                mainVideoContainer.removeViewAt(i);
            }
        }
    }

    private void moveLocalToThumbnail() {
        if (localSurface.getParent() != null) {
            ((FrameLayout) localSurface.getParent()).removeView(localSurface);
        }
        addThumbnailForLocal();
    }

    private void moveLocalToMainView() {
        removeLocalThumbnail();
        if (localSurface.getParent() != null) {
            ((FrameLayout) localSurface.getParent()).removeView(localSurface);
        }
        mainVideoContainer.addView(localSurface, 0,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
        mainViewUid = 0;
    }

    private void addThumbnailForUid(int uid, SurfaceView surface) {
        int sizePx = dpToPx(70);
        int marginPx = dpToPx(6);

        FrameLayout frame = new FrameLayout(this);
        frame.setTag(uid);
        frame.setBackgroundResource(R.drawable.bg_video_rounded);
        frame.setClipToOutline(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
        lp.setMarginEnd(marginPx);
        frame.setLayoutParams(lp);

        if (surface.getParent() != null) {
            ((FrameLayout) surface.getParent()).removeView(surface);
        }
        frame.addView(surface, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        TextView uidLabel = new TextView(this);
        uidLabel.setText("ID:" + uid);
        uidLabel.setTextColor(0xFFFFFFFF);
        uidLabel.setTextSize(9);
        uidLabel.setBackgroundColor(0x88000000);
        uidLabel.setPadding(4, 1, 4, 1);
        FrameLayout.LayoutParams labelLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        labelLp.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
        frame.addView(uidLabel, labelLp);

        thumbnailContainer.addView(frame);
    }

    private void addThumbnailForLocal() {
        int sizePx = dpToPx(70);
        int marginPx = dpToPx(6);

        FrameLayout frame = new FrameLayout(this);
        frame.setTag("local");
        frame.setBackgroundResource(R.drawable.bg_video_rounded);
        frame.setClipToOutline(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
        lp.setMarginEnd(marginPx);
        frame.setLayoutParams(lp);

        frame.addView(localSurface, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        TextView label = new TextView(this);
        label.setText("You");
        label.setTextColor(0xFFFFFFFF);
        label.setTextSize(9);
        label.setBackgroundColor(0x88000000);
        label.setPadding(4, 1, 4, 1);
        FrameLayout.LayoutParams labelLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        labelLp.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
        frame.addView(label, labelLp);

        thumbnailContainer.addView(frame, 0);
    }

    private void removeThumbnailForUid(int uid) {
        for (int i = 0; i < thumbnailContainer.getChildCount(); i++) {
            View child = thumbnailContainer.getChildAt(i);
            if (child.getTag() instanceof Integer && (int) child.getTag() == uid) {
                thumbnailContainer.removeViewAt(i);
                return;
            }
        }
    }

    private void removeLocalThumbnail() {
        for (int i = 0; i < thumbnailContainer.getChildCount(); i++) {
            View child = thumbnailContainer.getChildAt(i);
            if ("local".equals(child.getTag())) {
                thumbnailContainer.removeViewAt(i);
                return;
            }
        }
    }

    // Focus timer

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
                recordSessionComplete();
                showSessionCompletedDialog();
            }
        }.start();
    }

    private void updateTimerText(long millisLeft) {
        int totalSeconds = (int) (millisLeft / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String formatted = String.format(Locale.US, "%02d:%02d\nLeft", minutes, seconds);
        tvTimer.setText(formatted);
    }

    // Dialogs

    private void showSessionCompletedDialog() {
        if (isFinishing() || isDestroyed()) return;

        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_session_completed, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

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
            leaveAndGoHome(true, backendSessionId);
        });

        dialog.show();
    }

    private void showManualExitWarningDialog() {
        if (isFinishing() || isDestroyed()) return;

        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_exit_warning, null);

        ((TextView) view.findViewById(R.id.tvWarningMessage))
                .setText(R.string.manual_exit_warning);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        view.findViewById(R.id.btnContinueStudying).setOnClickListener(v -> dialog.dismiss());

        view.findViewById(R.id.btnLeaveSession).setOnClickListener(v -> {
            dialog.dismiss();
            if (!isSessionCompleted) recordSessionIncomplete();
            leaveAndGoHome(false);
        });

        dialog.show();
    }

    private void showDistractionWarningDialog() {
        if (isFinishing() || isDestroyed()) return;

        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_exit_warning, null);

        ((TextView) view.findViewById(R.id.tvWarningMessage))
                .setText(R.string.distraction_warning);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        view.findViewById(R.id.btnContinueStudying).setOnClickListener(v -> dialog.dismiss());

        view.findViewById(R.id.btnLeaveSession).setOnClickListener(v -> {
            dialog.dismiss();
            if (!isSessionCompleted) recordSessionIncomplete();
            leaveAndGoHome(false);
        });

        dialog.show();
    }

    // Navigation helpers

    private void leaveAndGoHome(boolean goToTasks) {
        leaveAndGoHome(goToTasks, -1L);
    }

    private void leaveAndGoHome(boolean goToTasks, long sessionIdForReview) {
        if (focusTimer != null) focusTimer.cancel();
        leaveAndCleanup();

        Intent intent = new Intent(this, MainHubActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (goToTasks) {
            intent.putExtra("navigate_to_tasks", true);
            if (sessionIdForReview > 0) {
                intent.putExtra("review_session_id", sessionIdForReview);
            }
        }
        startActivity(intent);
        finish();
    }

    private void restartFocusTimer() {
        isSessionCompleted = false;
        backendSessionId = -1;
        if (focusTimer != null) focusTimer.cancel();
        recordSessionStart();
        startFocusTimer();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
