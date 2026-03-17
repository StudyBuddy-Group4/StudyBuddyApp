package com.example.studybuddyapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.ModerationApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminMeetingRoomActivity extends AppCompatActivity {

    private static final String TAG = "AdminMeetingRoom";
    private static final int PERMISSION_REQ_ID = 200;

    private RtcEngine rtcEngine;
    private String channelName;
    private long reportId = -1;
    private long reportedUserId = -1;

    private LinearLayout participantGrid;
    private final List<Integer> remoteUids = new ArrayList<>();
    private final Map<Integer, SurfaceView> remoteSurfaces = new HashMap<>();

    private final IRtcEngineEventHandler rtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            Log.d(TAG, "Admin joined channel: " + channel);
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() -> addParticipant(uid));
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> removeParticipant(uid));
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
        setContentView(R.layout.activity_admin_meeting_room);

        channelName = getIntent().getStringExtra("channel_name");
        reportId = getIntent().getLongExtra("report_id", -1);
        reportedUserId = getIntent().getLongExtra("reported_user_id", -1);

        participantGrid = findViewById(R.id.participantGrid);

        findViewById(R.id.ivBack).setOnClickListener(v -> {
            leaveAndCleanup();
            finish();
        });

        if (channelName == null || channelName.isEmpty()) {
            Toast.makeText(this, "No meeting channel specified.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (checkPermissions()) {
            initAndJoinAsSpectator();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA},
                    PERMISSION_REQ_ID);
        }
    }

    @Override
    protected void onDestroy() {
        leaveAndCleanup();
        super.onDestroy();
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID && checkPermissions()) {
            initAndJoinAsSpectator();
        }
    }

    private void initAndJoinAsSpectator() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = AgoraConfig.APP_ID;
            config.mEventHandler = rtcEventHandler;
            rtcEngine = RtcEngine.create(config);
            rtcEngine.enableVideo();
        } catch (Exception e) {
            Log.e(TAG, "Failed to init RtcEngine", e);
            finish();
            return;
        }

        ChannelMediaOptions options = new ChannelMediaOptions();
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        options.publishCameraTrack = false;
        options.publishMicrophoneTrack = false;
        options.autoSubscribeAudio = true;
        options.autoSubscribeVideo = true;

        SessionManager session = new SessionManager(this);
        int uid = session.isLoggedIn() ? (int) session.getUserId() : 0;

        String token = AgoraConfig.TEMP_TOKEN.isEmpty() ? null : AgoraConfig.TEMP_TOKEN;
        rtcEngine.joinChannel(token, channelName, uid, options);
    }

    private void leaveAndCleanup() {
        if (rtcEngine != null) {
            rtcEngine.leaveChannel();
            RtcEngine.destroy();
            rtcEngine = null;
        }
    }

    private void addParticipant(int uid) {
        if (remoteUids.contains(uid)) return;
        remoteUids.add(uid);
        rebuildGrid();
    }

    private void removeParticipant(int uid) {
        remoteUids.remove(Integer.valueOf(uid));
        remoteSurfaces.remove(uid);
        rebuildGrid();
    }

    private void rebuildGrid() {
        participantGrid.removeAllViews();

        LinearLayout currentRow = null;
        int countInRow = 0;

        for (int i = 0; i < remoteUids.size(); i++) {
            if (countInRow == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowLp.bottomMargin = dpToPx(12);
                participantGrid.addView(currentRow, rowLp);
            }

            int uid = remoteUids.get(i);
            FrameLayout cell = createParticipantCell(uid);
            LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(
                    0, dpToPx(150), 1f);
            cellLp.setMarginEnd(countInRow == 0 ? dpToPx(6) : 0);
            cellLp.setMarginStart(countInRow == 1 ? dpToPx(6) : 0);
            currentRow.addView(cell, cellLp);

            countInRow++;
            if (countInRow == 2) countInRow = 0;
        }

        if (countInRow == 1 && currentRow != null) {
            View spacer = new View(this);
            LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(
                    0, dpToPx(150), 1f);
            spacerLp.setMarginStart(dpToPx(6));
            currentRow.addView(spacer, spacerLp);
        }
    }

    private FrameLayout createParticipantCell(int uid) {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundResource(R.drawable.bg_video_rounded);
        frame.setClipToOutline(true);

        SurfaceView surface = new SurfaceView(this);
        surface.setZOrderMediaOverlay(true);
        remoteSurfaces.put(uid, surface);
        frame.addView(surface, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        if (rtcEngine != null) {
            rtcEngine.setupRemoteVideo(new VideoCanvas(surface, VideoCanvas.RENDER_MODE_HIDDEN, uid));
        }

        TextView uidLabel = new TextView(this);
        uidLabel.setText("ID: " + uid);
        uidLabel.setTextColor(0xFFFFFFFF);
        uidLabel.setTextSize(11);
        uidLabel.setBackgroundColor(0x88000000);
        uidLabel.setPadding(8, 2, 8, 2);
        FrameLayout.LayoutParams labelLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        labelLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        labelLp.topMargin = dpToPx(6);
        frame.addView(uidLabel, labelLp);

        ImageView prohibitBtn = new ImageView(this);
        prohibitBtn.setImageResource(R.drawable.ic_prohibited);
        prohibitBtn.setContentDescription("Take action");
        FrameLayout.LayoutParams btnLp = new FrameLayout.LayoutParams(
                dpToPx(40), dpToPx(40));
        btnLp.gravity = Gravity.BOTTOM | Gravity.END;
        btnLp.setMargins(0, 0, dpToPx(8), dpToPx(8));
        frame.addView(prohibitBtn, btnLp);

        prohibitBtn.setOnClickListener(v -> showAdminDecisionDialog(uid));

        return frame;
    }

    private void showAdminDecisionDialog(int targetUid) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_admin_decision, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnKickOut).setOnClickListener(v -> {
            executeAdminAction(targetUid, "KICK");
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btnBan3Days).setOnClickListener(v -> {
            executeAdminAction(targetUid, "BAN_3_DAYS");
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btnBanPermanently).setOnClickListener(v -> {
            executeAdminAction(targetUid, "BAN_PERMANENT");
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btnBack).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void executeAdminAction(int targetUid, String actionType) {
        ModerationApi api = ApiClient.getModerationApi(this);

        api.applyAdminAction(targetUid, actionType).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminMeetingRoomActivity.this,
                            "Action " + actionType + " applied to user " + targetUid,
                            Toast.LENGTH_SHORT).show();
                    if (reportId > 0) {
                        markReportActioned();
                    }
                } else {
                    Toast.makeText(AdminMeetingRoomActivity.this,
                            "Server rejected the action.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(AdminMeetingRoomActivity.this,
                        "Network error.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markReportActioned() {
        ModerationApi api = ApiClient.getModerationApi(this);
        api.updateReportStatus(reportId, "ACTIONED").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d(TAG, "Report marked as ACTIONED");
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) { }
        });
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
