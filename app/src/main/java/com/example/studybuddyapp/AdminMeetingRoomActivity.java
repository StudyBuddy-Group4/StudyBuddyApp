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

/**
 * Lets an administrator observe a reported meeting room and apply moderation actions to participants.
 */
public class AdminMeetingRoomActivity extends AppCompatActivity {

    // Log tag
    private static final String TAG = "AdminMeetingRoom";
    // Permission request code
    private static final int PERMISSION_REQ_ID = 200;

    // Agora spectator engine
    private RtcEngine rtcEngine;
    // Room and moderation metadata
    private String channelName;
    private long reportId = -1;
    private long reportedUserId = -1;

    // Participant grid and cached remote surfaces
    private LinearLayout participantGrid;
    private final List<Integer> remoteUids = new ArrayList<>();
    private final Map<Integer, SurfaceView> remoteSurfaces = new HashMap<>();

    private final IRtcEngineEventHandler rtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            // The admin join event is only logged because no extra UI change is needed here.
            Log.d(TAG, "Admin joined channel: " + channel);
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            // Agora callbacks are marshalled back to the UI thread before touching the participant grid.
            runOnUiThread(() -> addParticipant(uid));
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            // Participant removal can change the grid structure, so it also runs on the UI thread.
            runOnUiThread(() -> removeParticipant(uid));
        }

        @Override
        public void onError(int err) {
            // Agora errors are logged for debugging but do not show a second admin dialog here.
            Log.e(TAG, "Agora error: " + err);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Keep the moderation screen awake while the admin is reviewing a live meeting.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_admin_meeting_room);

        // Read the room and report metadata passed from the moderation flow.
        channelName = getIntent().getStringExtra("channel_name");
        reportId = getIntent().getLongExtra("report_id", -1);
        reportedUserId = getIntent().getLongExtra("reported_user_id", -1);

        // Root container for runtime-generated participant cards
        participantGrid = findViewById(R.id.participantGrid);

        // The back arrow leaves moderation mode immediately.
        findViewById(R.id.ivBack).setOnClickListener(v -> {
            // Leaving this screen should always clean up the Agora spectator connection first.
            leaveAndCleanup();
            finish();
        });

        if (channelName == null || channelName.isEmpty()) {
            // Without a channel name the admin cannot attach to the reported room at all.
            Toast.makeText(this, "No meeting channel specified.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (checkPermissions()) {
            initAndJoinAsSpectator();
        } else {
            // Request the media permissions used by this spectator join flow before opening the room.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA},
                    PERMISSION_REQ_ID);
        }
    }

    @Override
    protected void onDestroy() {
        // Ensure Agora resources are released even if the activity is closed abruptly.
        leaveAndCleanup();
        super.onDestroy();
    }

    /**
     * Checks whether the minimum permissions needed for the spectator join flow are present.
     */
    private boolean checkPermissions() {
        // This implementation only gates room entry on camera permission.
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID && checkPermissions()) {
            // Retry the spectator join immediately once the required permission has been granted.
            initAndJoinAsSpectator();
        }
    }

    /**
     * Joins the reported meeting in a passive spectator configuration.
     */
    private void initAndJoinAsSpectator() {
        try {
            // Build a lightweight Agora engine for observation only.
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = AgoraConfig.APP_ID;
            config.mEventHandler = rtcEventHandler;
            rtcEngine = RtcEngine.create(config);
            // The admin still needs video enabled to subscribe to remote participant streams.
            rtcEngine.enableVideo();
        } catch (Exception e) {
            // If Agora cannot be created there is no useful spectator fallback.
            Log.e(TAG, "Failed to init RtcEngine", e);
            finish();
            return;
        }

        ChannelMediaOptions options = new ChannelMediaOptions();
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
        // The admin joins as a silent participant who only observes audio and video.
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        options.publishCameraTrack = false;
        options.publishMicrophoneTrack = false;
        options.autoSubscribeAudio = true;
        options.autoSubscribeVideo = true;

        SessionManager session = new SessionManager(this);
        // Reuse the logged-in admin id as the Agora uid when available.
        int uid = session.isLoggedIn() ? (int) session.getUserId() : 0;

        // Join the reported room with the shared temporary token setup.
        String token = AgoraConfig.TEMP_TOKEN.isEmpty() ? null : AgoraConfig.TEMP_TOKEN;
        // Joining starts the live spectator session for the current moderation target.
        rtcEngine.joinChannel(token, channelName, uid, options);
    }

    /**
     * Leaves the Agora channel and destroys the engine instance.
     */
    private void leaveAndCleanup() {
        if (rtcEngine != null) {
            // The spectator leaves cleanly before the global engine is destroyed.
            rtcEngine.leaveChannel();
            RtcEngine.destroy();
            rtcEngine = null;
        }
    }

    /**
     * Adds a remote participant to the local list and rebuilds the grid layout.
     */
    private void addParticipant(int uid) {
        if (remoteUids.contains(uid)) return;
        // Track unique participants only once to avoid duplicate grid cells.
        remoteUids.add(uid);
        rebuildGrid();
    }

    /**
     * Removes a participant from the grid and surface cache.
     */
    private void removeParticipant(int uid) {
        remoteUids.remove(Integer.valueOf(uid));
        remoteSurfaces.remove(uid);
        // Rebuild the grid after every removal so spacing and row structure stay correct.
        rebuildGrid();
    }

    /**
     * Recreates the two-column participant grid shown to the moderator.
     */
    private void rebuildGrid() {
        // Rebuild from scratch because participant cards are created dynamically in code.
        participantGrid.removeAllViews();

        // Two cards are placed in each horizontal row.
        LinearLayout currentRow = null;
        int countInRow = 0;

        for (int i = 0; i < remoteUids.size(); i++) {
            if (countInRow == 0) {
                // Start a new horizontal row every two participants.
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
            // Equal weights keep both columns the same width.
            LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(
                    0, dpToPx(150), 1f);
            cellLp.setMarginEnd(countInRow == 0 ? dpToPx(6) : 0);
            cellLp.setMarginStart(countInRow == 1 ? dpToPx(6) : 0);
            currentRow.addView(cell, cellLp);

            countInRow++;
            if (countInRow == 2) countInRow = 0;
        }

        if (countInRow == 1 && currentRow != null) {
            // Pad out the last row so a single participant still aligns like a two-column grid.
            View spacer = new View(this);
            LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(
                    0, dpToPx(150), 1f);
            spacerLp.setMarginStart(dpToPx(6));
            currentRow.addView(spacer, spacerLp);
        }
    }

    /**
     * Builds a single participant card with video, uid label, and moderation action button.
     */
    private FrameLayout createParticipantCell(int uid) {
        FrameLayout frame = new FrameLayout(this);
        // Reuse the rounded video background from the regular meeting UI.
        frame.setBackgroundResource(R.drawable.bg_video_rounded);
        frame.setClipToOutline(true);

        SurfaceView surface = new SurfaceView(this);
        surface.setZOrderMediaOverlay(true);
        // Cache the surface by uid so future grid rebuilds can replace the correct participant cell.
        remoteSurfaces.put(uid, surface);
        frame.addView(surface, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        if (rtcEngine != null) {
            // Remote video is rebound whenever the grid is rebuilt.
            rtcEngine.setupRemoteVideo(new VideoCanvas(surface, VideoCanvas.RENDER_MODE_HIDDEN, uid));
        }

        TextView uidLabel = new TextView(this);
        // Show the remote uid in the tile so the admin can match actions to participants.
        uidLabel.setText("ID: " + uid);
        uidLabel.setTextColor(0xFFFFFFFF);
        uidLabel.setTextSize(11);
        uidLabel.setBackgroundColor(0x88000000);
        uidLabel.setPadding(8, 2, 8, 2);
        FrameLayout.LayoutParams labelLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        // The uid badge stays near the top edge of the participant tile.
        labelLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        labelLp.topMargin = dpToPx(6);
        frame.addView(uidLabel, labelLp);

        ImageView prohibitBtn = new ImageView(this);
        // The same action icon is reused for all moderation choices.
        prohibitBtn.setImageResource(R.drawable.ic_prohibited);
        prohibitBtn.setContentDescription("Take action");
        FrameLayout.LayoutParams btnLp = new FrameLayout.LayoutParams(
                dpToPx(40), dpToPx(40));
        // The moderation button stays pinned to the lower-right corner of the tile.
        btnLp.gravity = Gravity.BOTTOM | Gravity.END;
        btnLp.setMargins(0, 0, dpToPx(8), dpToPx(8));
        frame.addView(prohibitBtn, btnLp);

        // Each participant card opens the same decision dialog with the selected target uid.
        prohibitBtn.setOnClickListener(v -> showAdminDecisionDialog(uid));

        return frame;
    }

    /**
     * Shows the moderation decision dialog for a selected participant.
     */
    private void showAdminDecisionDialog(int targetUid) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_admin_decision, null);
        // One shared dialog keeps the moderation options consistent for every participant tile.

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            // The custom moderation dialog uses its own background styling.
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnKickOut).setOnClickListener(v -> {
            // Kick is the lightest moderation action available from this dialog.
            executeAdminAction(targetUid, "KICK");
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btnBan3Days).setOnClickListener(v -> {
            // Temporary bans and permanent bans share the same backend action endpoint.
            executeAdminAction(targetUid, "BAN_3_DAYS");
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btnBanPermanently).setOnClickListener(v -> {
            // Permanent bans use the same backend endpoint with a different action type.
            executeAdminAction(targetUid, "BAN_PERMANENT");
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btnBack).setOnClickListener(v -> dialog.dismiss());
        // Backing out of the dialog leaves the admin connected to the room without applying any action.

        // The dialog only appears after all handlers have been attached.
        dialog.show();
    }

    /**
     * Sends the selected moderation action to the backend.
     */
    private void executeAdminAction(int targetUid, String actionType) {
        ModerationApi api = ApiClient.getModerationApi(this);

        // Apply the chosen moderation action first.
        api.applyAdminAction(targetUid, actionType).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Successful actions optionally mark the linked report as resolved.
                    Toast.makeText(AdminMeetingRoomActivity.this,
                            "Action " + actionType + " applied to user " + targetUid,
                            Toast.LENGTH_SHORT).show();
                    if (reportId > 0) {
                        // Only reports that originated from moderation review need a follow-up status update.
                        markReportActioned();
                    }
                } else {
                    // Backend rejection usually indicates an invalid state or already-handled participant.
                    Toast.makeText(AdminMeetingRoomActivity.this,
                            "Server rejected the action.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Network errors leave the admin on the same room so they can retry the action.
                Toast.makeText(AdminMeetingRoomActivity.this,
                        "Network error.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Marks the originating moderation report as actioned after a successful admin action.
     */
    private void markReportActioned() {
        ModerationApi api = ApiClient.getModerationApi(this);
        // This secondary call keeps the report list aligned with what the admin already did in the room.
        api.updateReportStatus(reportId, "ACTIONED").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // No extra UI is needed here because the moderator already saw the action toast.
                Log.d(TAG, "Report marked as ACTIONED");
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Report-status sync failures are non-blocking, so they are intentionally ignored here.
            }
        });
    }

    /**
     * Converts dp units for the runtime-generated participant grid.
     */
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
