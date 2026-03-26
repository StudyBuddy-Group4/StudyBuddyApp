package com.example.studybuddyapp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.MatchingApi;
import com.example.studybuddyapp.api.SessionApi;
import com.example.studybuddyapp.api.TaskApi;
import com.example.studybuddyapp.api.UserApi;
import com.example.studybuddyapp.api.dto.AssignTasksRequest;
import com.example.studybuddyapp.api.dto.LeaveMeetingResponse;
import com.example.studybuddyapp.api.dto.StartSessionRequest;
import com.example.studybuddyapp.api.dto.StartSessionResponse;
import com.example.studybuddyapp.api.dto.UserProfileResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

class MeetingRoomSessionCoordinator {

    interface Callbacks {
        boolean shouldHandleCallbacks();
        void onUserRestricted(UserProfileResponse profile);
    }

    private static final String TAG = "MeetingRoom";
    private static final long BAN_CHECK_INTERVAL_MS = 5000L;

    private final Context appContext;
    private final String channelName;
    private final int focusDurationMinutes;
    private final Callbacks callbacks;

    private long backendSessionId = -1;
    private boolean isInChannel = false;
    private boolean matchingLeaveNotified = false;
    private Handler banCheckHandler;

    MeetingRoomSessionCoordinator(Context context,
                                  String channelName,
                                  int focusDurationMinutes,
                                  Callbacks callbacks) {
        this.appContext = context.getApplicationContext();
        this.channelName = channelName;
        this.focusDurationMinutes = focusDurationMinutes;
        this.callbacks = callbacks;
    }

    void onChannelJoined() {
        isInChannel = true;
        recordSessionStart();
        startBanPolling();
    }

    void onMeetingEnded() {
        isInChannel = false;
        stopBanPolling();
        notifyMatchingLeave();
    }

    void recordSessionComplete() {
        if (backendSessionId < 0) {
            return;
        }
        SessionApi api = ApiClient.getSessionApi(appContext);
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

    void recordSessionIncomplete() {
        if (backendSessionId < 0) {
            return;
        }
        SessionApi api = ApiClient.getSessionApi(appContext);
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

    void restartSession() {
        backendSessionId = -1;
        recordSessionStart();
    }

    private void startBanPolling() {
        stopBanPolling();
        banCheckHandler = new Handler(Looper.getMainLooper());
        banCheckHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isInChannel) {
                    return;
                }
                checkBanStatus();
                if (banCheckHandler != null) {
                    banCheckHandler.postDelayed(this, BAN_CHECK_INTERVAL_MS);
                }
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
        UserApi api = ApiClient.getUserApi(appContext);
        api.getProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call,
                                   Response<UserProfileResponse> response) {
                if (!isInChannel || !callbacks.shouldHandleCallbacks()) {
                    return;
                }
                UserProfileResponse profile = response.body();
                if (response.isSuccessful() && profile != null && profile.isCurrentlyRestricted()) {
                    stopBanPolling();
                    callbacks.onUserRestricted(profile);
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                Log.d(TAG, "Ban check failed, retrying on next poll", t);
            }
        });
    }

    private void recordSessionStart() {
        SessionApi api = ApiClient.getSessionApi(appContext);
        StartSessionRequest request = new StartSessionRequest(focusDurationMinutes, channelName);
        api.startSession(request).enqueue(new Callback<StartSessionResponse>() {
            @Override
            public void onResponse(Call<StartSessionResponse> call,
                                   Response<StartSessionResponse> response) {
                StartSessionResponse body = response.body();
                if (response.isSuccessful() && body != null) {
                    backendSessionId = body.getSessionId();
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
        TaskApi taskApi = ApiClient.getTaskApi(appContext);
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

    private void notifyMatchingLeave() {
        if (matchingLeaveNotified || channelName == null || channelName.isEmpty()) {
            return;
        }
        matchingLeaveNotified = true;

        MatchingApi matchingApi = ApiClient.getMatchingApi(appContext);
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
}
