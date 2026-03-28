package com.example.studybuddyapp;

import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
import io.agora.rtc2.video.VideoEncoderConfiguration;

/**
 * Encapsulates Agora setup and the local/remote video layout logic used inside the meeting room.
 */
class MeetingRoomVideoCallManager {
    private static final int VIDEO_RENDER_MODE = VideoCanvas.RENDER_MODE_FIT;

    /**
     * Reports Agora lifecycle events back to the activity that owns the meeting UI.
     */
    interface Callbacks {
        void onChannelJoined();
        void onMainViewChanged(int uid);
    }

    private static final String TAG = "MeetingRoom";

    private final AppCompatActivity activity;
    private FrameLayout mainVideoContainer;
    private LinearLayout thumbnailContainer;
    private final Callbacks callbacks;

    private final List<Integer> remoteUids = new ArrayList<>();
    private final Map<Integer, SurfaceView> remoteSurfaces = new HashMap<>();

    private RtcEngine rtcEngine;
    private SurfaceView localSurface;
    private boolean isInChannel = false;
    private int mainViewUid = 0;

    private final IRtcEngineEventHandler rtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            Log.d(TAG, "Joined channel: " + channel + " uid=" + uid);
            activity.runOnUiThread(() -> {
                // Agora callbacks arrive off the UI thread, so UI state changes are posted back safely.
                isInChannel = true;
                callbacks.onChannelJoined();
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            Log.d(TAG, "Remote user joined: " + uid);
            // Participant layout changes always happen on the activity UI thread.
            activity.runOnUiThread(() -> addRemoteUser(uid));
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            Log.d(TAG, "Remote user offline: " + uid);
            // Removing a participant may also promote another surface into the main view.
            activity.runOnUiThread(() -> removeRemoteUser(uid));
        }

        @Override
        public void onError(int err) {
            // Agora errors are only logged here; user-facing failures are handled near the triggering action.
            Log.e(TAG, "Agora error: " + err);
        }
    };

    MeetingRoomVideoCallManager(AppCompatActivity activity,
                                FrameLayout mainVideoContainer,
                                LinearLayout thumbnailContainer,
                                Callbacks callbacks) {
        this.activity = activity;
        this.mainVideoContainer = mainVideoContainer;
        this.thumbnailContainer = thumbnailContainer;
        this.callbacks = callbacks;
    }

    /**
     * Initialises Agora, shows the local preview, and joins the requested channel.
     */
    boolean initAndJoin(String channelName, int uid) {
        if (AgoraConfig.APP_ID.isEmpty()) {
            // Joining cannot proceed until the local Agora configuration has been filled in.
            Toast.makeText(activity,
                    "Please set your Agora App ID in AgoraConfig.java",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        if (!initRtcEngine()) {
            return false;
        }

        // Show the local preview before the remote join completes so the screen feels responsive.
        setupLocalPreview();
        joinChannel(channelName, uid);
        return true;
    }

    /**
     * Returns whether Agora has confirmed a successful channel join.
     */
    boolean isInChannel() {
        return isInChannel;
    }

    /**
     * Returns a stable snapshot of the current remote participant ids.
     */
    ArrayList<Integer> getRemoteUidsSnapshot() {
        return new ArrayList<>(remoteUids);
    }

    /**
     * Reattaches the video surfaces after the activity layout is recreated.
     */
    void rebindContainers(FrameLayout mainVideoContainer, LinearLayout thumbnailContainer) {
        this.mainVideoContainer = mainVideoContainer;
        this.thumbnailContainer = thumbnailContainer;
        // Layout recreation requires every surface to be attached to the new containers.
        rebuildVideoLayout();
    }

    /**
     * Toggles the local microphone stream.
     */
    void setMicMuted(boolean isMuted) {
        if (rtcEngine != null) {
            rtcEngine.muteLocalAudioStream(isMuted);
        }
    }

    /**
     * Toggles the local camera stream and hides the local preview when needed.
     */
    void setCameraOff(boolean isCameraOff) {
        if (rtcEngine != null) {
            rtcEngine.muteLocalVideoStream(isCameraOff);
        }
        if (localSurface != null) {
            localSurface.setVisibility(isCameraOff ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Toggles speakerphone output for the device.
     */
    void setSpeakerOff(boolean isSpeakerOff) {
        if (rtcEngine != null) {
            // The stored UI flag is inverted because Agora expects an "enabled" value here.
            rtcEngine.setEnableSpeakerphone(!isSpeakerOff);
        }
    }

    /**
     * Leaves the channel and releases all Agora-related resources.
     */
    void leaveAndCleanup() {
        // Clear local tracking first so late callbacks do not rebuild stale UI.
        isInChannel = false;
        remoteUids.clear();
        remoteSurfaces.clear();
        if (rtcEngine != null) {
            // Stop previewing before leaving to avoid keeping the camera active.
            rtcEngine.stopPreview();
            rtcEngine.leaveChannel();
            RtcEngine.destroy();
            rtcEngine = null;
        }
    }

    /**
     * Creates and configures the Agora engine instance.
     */
    private boolean initRtcEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = activity.getApplicationContext();
            config.mAppId = AgoraConfig.APP_ID;
            config.mEventHandler = rtcEventHandler;
            rtcEngine = RtcEngine.create(config);

            // The study room always expects a video call with modest quality to keep it lightweight.
            rtcEngine.enableVideo();
            rtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_640x360,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
            ));
            // Speakerphone is enabled by default so users do not need to switch audio output manually.
            rtcEngine.setEnableSpeakerphone(true);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to init RtcEngine", e);
            Toast.makeText(activity, "Error initialising video engine", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    /**
     * Builds the initial local preview before any remote user joins the channel.
     */
    private void setupLocalPreview() {
        localSurface = createLocalSurface();
        attachLocalSurfaceToMain();
        // StartPreview lets the local user immediately see their own video before others join.
        rtcEngine.startPreview();
        mainViewUid = 0;
    }

    /**
     * Creates the local video surface and binds it to Agora.
     */
    private SurfaceView createLocalSurface() {
        SurfaceView surface = new SurfaceView(activity);
        // Agora uses uid 0 to represent the local preview canvas.
        rtcEngine.setupLocalVideo(new VideoCanvas(surface, VIDEO_RENDER_MODE, 0));
        return surface;
    }

    /**
     * Places the local preview in the large main video container.
     */
    private void attachLocalSurfaceToMain() {
        // Detach first because the same SurfaceView may be moved between multiple parents.
        detachSurface(localSurface);
        mainVideoContainer.addView(localSurface, 0, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
    }

    /**
     * Joins the target channel using broadcaster settings so local audio and video are published.
     */
    private void joinChannel(String channelName, int uid) {
        // A null token is valid for the temporary setup used by this project configuration.
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
            // Surface immediate join failures to the user instead of silently staying on a blank screen.
            Toast.makeText(activity, "Failed to join channel (code " + result + ")",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Adds a newly joined remote user into the current layout.
     */
    private void addRemoteUser(int uid) {
        if (remoteUids.contains(uid)) {
            // Duplicate join callbacks are ignored to keep the layout stable.
            return;
        }
        remoteUids.add(uid);

        SurfaceView surface = createRemoteSurface(uid);

        // The first remote participant is promoted into the main view while the local preview moves aside.
        if (mainViewUid == 0 && remoteUids.size() == 1) {
            promoteToMainView(uid);
            moveLocalToThumbnail();
        } else {
            // Later participants are appended as thumbnails.
            addThumbnailForUid(uid, surface);
        }
    }

    /**
     * Removes a remote participant and restores the main view if necessary.
     */
    private void removeRemoteUser(int uid) {
        remoteUids.remove(Integer.valueOf(uid));
        remoteSurfaces.remove(uid);
        removeThumbnailForUid(uid);

        if (mainViewUid != uid) {
            // If the user was only in the thumbnail strip, the main view does not need to change.
            return;
        }

        clearMainView();
        if (!remoteUids.isEmpty()) {
            // Promote the next remaining participant when the current main remote user leaves.
            int nextUid = remoteUids.get(0);
            removeThumbnailForUid(nextUid);
            promoteToMainView(nextUid);
        } else {
            // If no remote participants remain, return the local preview to the main view.
            moveLocalToMainView();
        }
    }

    /**
     * Moves a remote participant surface into the main video area.
     */
    private void promoteToMainView(int uid) {
        SurfaceView surface = remoteSurfaces.get(uid);
        if (surface == null) {
            // Missing surfaces can happen if layout rebuilding races with a leave event.
            return;
        }
        detachSurface(surface);
        mainVideoContainer.addView(surface, 0, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        mainViewUid = uid;
        callbacks.onMainViewChanged(uid);
    }

    /**
     * Removes any video surface currently occupying the main container.
     */
    private void clearMainView() {
        for (int i = mainVideoContainer.getChildCount() - 1; i >= 0; i--) {
            View child = mainVideoContainer.getChildAt(i);
            if (child instanceof SurfaceView) {
                // Only remove runtime-added video surfaces and leave any non-video decoration alone.
                mainVideoContainer.removeViewAt(i);
            }
        }
    }

    /**
     * Moves the local preview into the thumbnail strip.
     */
    private void moveLocalToThumbnail() {
        detachSurface(localSurface);
        addThumbnailForLocal();
    }

    /**
     * Restores the local preview to the main view when no remote user should occupy it.
     */
    private void moveLocalToMainView() {
        removeLocalThumbnail();
        attachLocalSurfaceToMain();
        mainViewUid = 0;
        callbacks.onMainViewChanged(0);
    }

    /**
     * Adds a remote user's surface to the thumbnail strip.
     */
    private void addThumbnailForUid(int uid, SurfaceView surface) {
        FrameLayout frame = createThumbnailFrame(uid);
        detachSurface(surface);
        frame.addView(surface, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        // The label keeps remote thumbnails identifiable even when video is small.
        addLabelToFrame(frame, "ID:" + uid);
        // Remote thumbnails are appended after any local thumbnail already shown at index 0.
        thumbnailContainer.addView(frame);
    }

    /**
     * Adds the local preview as a thumbnail when a remote participant is in the main view.
     */
    private void addThumbnailForLocal() {
        FrameLayout frame = createThumbnailFrame("local");
        detachSurface(localSurface);
        frame.addView(localSurface, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        // The local preview keeps a simple "You" label instead of an Agora uid.
        addLabelToFrame(frame, "You");
        // Keep the local preview first in the strip for a stable visual layout.
        thumbnailContainer.addView(frame, 0);
    }

    /**
     * Creates the shared thumbnail container used for local and remote video previews.
     */
    private FrameLayout createThumbnailFrame(Object tag) {
        int sizePx = dpToPx(70);
        int marginPx = dpToPx(6);

        FrameLayout frame = new FrameLayout(activity);
        // The tag is later used to find and replace specific thumbnail entries.
        frame.setTag(tag);
        frame.setBackgroundResource(R.drawable.bg_video_rounded);
        frame.setClipToOutline(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
        lp.setMarginEnd(marginPx);
        frame.setLayoutParams(lp);
        return frame;
    }

    /**
     * Adds a small text label so thumbnails still identify the local user or remote uid.
     */
    private void addLabelToFrame(FrameLayout frame, String labelText) {
        TextView label = new TextView(activity);
        label.setText(labelText);
        label.setTextColor(0xFFFFFFFF);
        label.setTextSize(9);
        label.setBackgroundColor(0x88000000);
        label.setPadding(4, 1, 4, 1);
        FrameLayout.LayoutParams labelLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        labelLp.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
        frame.addView(label, labelLp);
    }

    /**
     * Removes the thumbnail that belongs to the given remote uid.
     */
    private void removeThumbnailForUid(int uid) {
        for (int i = 0; i < thumbnailContainer.getChildCount(); i++) {
            View child = thumbnailContainer.getChildAt(i);
            if (child.getTag() instanceof Integer && (int) child.getTag() == uid) {
                // Remove only the matching participant thumbnail and leave the rest untouched.
                thumbnailContainer.removeViewAt(i);
                return;
            }
        }
    }

    /**
     * Removes the local thumbnail when the local preview returns to the main view.
     */
    private void removeLocalThumbnail() {
        for (int i = 0; i < thumbnailContainer.getChildCount(); i++) {
            View child = thumbnailContainer.getChildAt(i);
            if ("local".equals(child.getTag())) {
                // The local preview uses a fixed string tag to distinguish it from remote uids.
                thumbnailContainer.removeViewAt(i);
                return;
            }
        }
    }

    /**
     * Detaches a surface from its current parent before it is reattached elsewhere.
     */
    private void detachSurface(SurfaceView surface) {
        if (surface != null && surface.getParent() instanceof ViewGroup) {
            ((ViewGroup) surface.getParent()).removeView(surface);
        }
    }

    /**
     * Reconstructs the full video layout after configuration changes.
     */
    private void rebuildVideoLayout() {
        if (mainVideoContainer == null || thumbnailContainer == null) {
            return;
        }
        // Start from a clean slate because the old view hierarchy belonged to the previous layout instance.
        clearMainView();
        thumbnailContainer.removeAllViews();

        if (rtcEngine != null && localSurface != null) {
            // A fresh surface is created after recreation so Agora can bind it to the new hierarchy.
            localSurface = createLocalSurface();
        }

        if (localSurface != null) {
            if (mainViewUid == 0) {
                // Preserve whether the local preview belonged in the main view before recreation.
                attachLocalSurfaceToMain();
            } else {
                addThumbnailForLocal();
            }
        }

        for (int uid : remoteUids) {
            // Recreate or reuse every known remote participant surface after rotation.
            SurfaceView surface = rtcEngine != null ? createRemoteSurface(uid) : remoteSurfaces.get(uid);
            if (surface == null) continue;
            if (uid == mainViewUid) {
                promoteToMainView(uid);
            } else {
                addThumbnailForUid(uid, surface);
            }
        }
    }

    /**
     * Creates a remote surface and binds it to the matching Agora uid.
     */
    private SurfaceView createRemoteSurface(int uid) {
        SurfaceView surface = new SurfaceView(activity);
        // Cache the surface immediately so later promotion/removal logic can find it by uid.
        remoteSurfaces.put(uid, surface);
        rtcEngine.setupRemoteVideo(new VideoCanvas(surface, VIDEO_RENDER_MODE, uid));
        return surface;
    }

    /**
     * Converts density-independent pixels into actual pixels for runtime-generated views.
     */
    private int dpToPx(int dp) {
        return Math.round(dp * activity.getResources().getDisplayMetrics().density);
    }
}
