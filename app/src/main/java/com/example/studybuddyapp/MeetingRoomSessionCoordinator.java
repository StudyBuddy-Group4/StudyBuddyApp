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

/**
 * Coordinates backend session tracking, task assignment, leave notifications, and restriction polling.
 */
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

    /**
     * Starts backend tracking after Agora confirms that the user joined the channel.
     */
    void onChannelJoined() {
        isInChannel = true;
        recordSessionStart();
        startBanPolling();
    }

    /**
     * Stops backend polling and notifies the matching service when the meeting is over.
     */
    void onMeetingEnded() {
        isInChannel = false;
        stopBanPolling();
        notifyMatchingLeave();
    }

    /**
     * Marks the current backend session as completed when the focus timer finishes.
     */
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

    /**
     * Marks the current backend session as incomplete when the user leaves early.
     */
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

    /**
     * Starts a fresh backend session record for another focus round in the same room.
     */
    void restartSession() {
        backendSessionId = -1;
        recordSessionStart();
    }

    /**
     * Polls the backend for restriction updates while the user remains in the meeting.
     */
    private void startBanPolling() {
        stopBanPolling();
        banCheckHandler = new Handler(Looper.getMainLooper());
        banCheckHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isInChannel) {
                    return;
                }
                // Polling stays lightweight and repeats on a fixed interval until the meeting ends.
                checkBanStatus();
                if (banCheckHandler != null) {
                    banCheckHandler.postDelayed(this, BAN_CHECK_INTERVAL_MS);
                }
            }
        }, BAN_CHECK_INTERVAL_MS);
    }

    /**
     * Cancels any pending restriction checks.
     */
    private void stopBanPolling() {
        if (banCheckHandler != null) {
            banCheckHandler.removeCallbacksAndMessages(null);
            banCheckHandler = null;
        }
    }

    /**
     * Checks whether the backend has restricted the user during the active session.
     */
    private void checkBanStatus() {
        UserApi api = ApiClient.getUserApi(appContext);
        api.getProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call,
                                   Response<UserProfileResponse> response) {
                // Ignore late callbacks when the room has already ended or the activity is gone.
                if (!isInChannel || !callbacks.shouldHandleCallbacks()) {
                    return;
                }
                UserProfileResponse profile = response.body();
                if (response.isSuccessful() && profile != null && profile.isCurrentlyRestricted()) {
                    // Once restricted, stop polling and let the activity decide how to remove the user.
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

    /**
     * Creates the backend session record used for completion tracking and task assignment.
     */
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
                    // Task assignment only happens after the backend has created a concrete session id.
                    assignPendingTasks(backendSessionId);
                }
            }

            @Override
            public void onFailure(Call<StartSessionResponse> call, Throwable t) {
                Log.w(TAG, "Failed to record session start", t);
            }
        });
    }

    /**
     * Associates any pending tasks with the current backend session.
     */
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

    /**
     * Tells the matching service that this user has left the room.
     */
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
