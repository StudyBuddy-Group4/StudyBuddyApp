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

class MeetingRoomVideoCallManager {
    private static final int VIDEO_RENDER_MODE = VideoCanvas.RENDER_MODE_FIT;

    interface Callbacks {
        void onChannelJoined();
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
                isInChannel = true;
                callbacks.onChannelJoined();
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            Log.d(TAG, "Remote user joined: " + uid);
            activity.runOnUiThread(() -> addRemoteUser(uid));
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            Log.d(TAG, "Remote user offline: " + uid);
            activity.runOnUiThread(() -> removeRemoteUser(uid));
        }

        @Override
        public void onError(int err) {
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

    boolean initAndJoin(String channelName, int uid) {
        if (AgoraConfig.APP_ID.isEmpty()) {
            Toast.makeText(activity,
                    "Please set your Agora App ID in AgoraConfig.java",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        if (!initRtcEngine()) {
            return false;
        }

        setupLocalPreview();
        joinChannel(channelName, uid);
        return true;
    }

    boolean isInChannel() {
        return isInChannel;
    }

    ArrayList<Integer> getRemoteUidsSnapshot() {
        return new ArrayList<>(remoteUids);
    }

    void rebindContainers(FrameLayout mainVideoContainer, LinearLayout thumbnailContainer) {
        this.mainVideoContainer = mainVideoContainer;
        this.thumbnailContainer = thumbnailContainer;
        rebuildVideoLayout();
    }

    void setMicMuted(boolean isMuted) {
        if (rtcEngine != null) {
            rtcEngine.muteLocalAudioStream(isMuted);
        }
    }

    void setCameraOff(boolean isCameraOff) {
        if (rtcEngine != null) {
            rtcEngine.muteLocalVideoStream(isCameraOff);
        }
        if (localSurface != null) {
            localSurface.setVisibility(isCameraOff ? View.GONE : View.VISIBLE);
        }
    }

    void setSpeakerOff(boolean isSpeakerOff) {
        if (rtcEngine != null) {
            rtcEngine.setEnableSpeakerphone(!isSpeakerOff);
        }
    }

    void leaveAndCleanup() {
        isInChannel = false;
        remoteUids.clear();
        remoteSurfaces.clear();
        if (rtcEngine != null) {
            rtcEngine.stopPreview();
            rtcEngine.leaveChannel();
            RtcEngine.destroy();
            rtcEngine = null;
        }
    }

    private boolean initRtcEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = activity.getApplicationContext();
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
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to init RtcEngine", e);
            Toast.makeText(activity, "Error initialising video engine", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void setupLocalPreview() {
        localSurface = createLocalSurface();
        attachLocalSurfaceToMain();
        rtcEngine.startPreview();
        mainViewUid = 0;
    }

    private SurfaceView createLocalSurface() {
        SurfaceView surface = new SurfaceView(activity);
        rtcEngine.setupLocalVideo(new VideoCanvas(surface, VIDEO_RENDER_MODE, 0));
        return surface;
    }

    private void attachLocalSurfaceToMain() {
        detachSurface(localSurface);
        mainVideoContainer.addView(localSurface, 0, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void joinChannel(String channelName, int uid) {
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
            Toast.makeText(activity, "Failed to join channel (code " + result + ")",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void addRemoteUser(int uid) {
        if (remoteUids.contains(uid)) {
            return;
        }
        remoteUids.add(uid);

        SurfaceView surface = createRemoteSurface(uid);

        if (mainViewUid == 0 && remoteUids.size() == 1) {
            promoteToMainView(uid);
            moveLocalToThumbnail();
        } else {
            addThumbnailForUid(uid, surface);
        }
    }

    private void removeRemoteUser(int uid) {
        remoteUids.remove(Integer.valueOf(uid));
        remoteSurfaces.remove(uid);
        removeThumbnailForUid(uid);

        if (mainViewUid != uid) {
            return;
        }

        clearMainView();
        if (!remoteUids.isEmpty()) {
            int nextUid = remoteUids.get(0);
            removeThumbnailForUid(nextUid);
            promoteToMainView(nextUid);
        } else {
            moveLocalToMainView();
        }
    }

    private void promoteToMainView(int uid) {
        SurfaceView surface = remoteSurfaces.get(uid);
        if (surface == null) {
            return;
        }
        detachSurface(surface);
        mainVideoContainer.addView(surface, 0, new FrameLayout.LayoutParams(
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
        detachSurface(localSurface);
        addThumbnailForLocal();
    }

    private void moveLocalToMainView() {
        removeLocalThumbnail();
        attachLocalSurfaceToMain();
        mainViewUid = 0;
    }

    private void addThumbnailForUid(int uid, SurfaceView surface) {
        FrameLayout frame = createThumbnailFrame(uid, "ID:" + uid);
        detachSurface(surface);
        frame.addView(surface, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        thumbnailContainer.addView(frame);
    }

    private void addThumbnailForLocal() {
        FrameLayout frame = createThumbnailFrame("local", "You");
        detachSurface(localSurface);
        frame.addView(localSurface, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        thumbnailContainer.addView(frame, 0);
    }

    private FrameLayout createThumbnailFrame(Object tag, String labelText) {
        int sizePx = dpToPx(70);
        int marginPx = dpToPx(6);

        FrameLayout frame = new FrameLayout(activity);
        frame.setTag(tag);
        frame.setBackgroundResource(R.drawable.bg_video_rounded);
        frame.setClipToOutline(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
        lp.setMarginEnd(marginPx);
        frame.setLayoutParams(lp);

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
        return frame;
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

    private void detachSurface(SurfaceView surface) {
        if (surface != null && surface.getParent() instanceof ViewGroup) {
            ((ViewGroup) surface.getParent()).removeView(surface);
        }
    }

    private void rebuildVideoLayout() {
        if (mainVideoContainer == null || thumbnailContainer == null) {
            return;
        }
        clearMainView();
        thumbnailContainer.removeAllViews();

        if (rtcEngine != null && localSurface != null) {
            localSurface = createLocalSurface();
        }

        if (localSurface != null) {
            if (mainViewUid == 0) {
                attachLocalSurfaceToMain();
            } else {
                addThumbnailForLocal();
            }
        }

        for (int uid : remoteUids) {
            SurfaceView surface = rtcEngine != null ? createRemoteSurface(uid) : remoteSurfaces.get(uid);
            if (surface == null) continue;
            if (uid == mainViewUid) {
                promoteToMainView(uid);
            } else {
                addThumbnailForUid(uid, surface);
            }
        }
    }

    private SurfaceView createRemoteSurface(int uid) {
        SurfaceView surface = new SurfaceView(activity);
        remoteSurfaces.put(uid, surface);
        rtcEngine.setupRemoteVideo(new VideoCanvas(surface, VIDEO_RENDER_MODE, uid));
        return surface;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * activity.getResources().getDisplayMetrics().density);
    }
}
