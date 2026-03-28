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

    /**
     * Allows the coordinator to delegate UI decisions back to the owning activity.
     */
    interface Callbacks {
        boolean shouldHandleCallbacks();
        void onUserRestricted(UserProfileResponse profile);
    }

    private static final String TAG = "MeetingRoom";
    // Restriction poll interval
    private static final long BAN_CHECK_INTERVAL_MS = 5000L;

    // Stable room/session inputs
    private final Context appContext;
    private final String channelName;
    private final int focusDurationMinutes;
    private final Callbacks callbacks;

    // Backend session state
    private long backendSessionId = -1;
    private boolean isInChannel = false;
    private boolean matchingLeaveNotified = false;
    // Main-thread poll handler
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
        // Joining the room is the point where backend session bookkeeping should begin.
        isInChannel = true;
        recordSessionStart();
        // Restriction checks begin only after the room is live.
        startBanPolling();
    }

    /**
     * Stops backend polling and notifies the matching service when the meeting is over.
     */
    void onMeetingEnded() {
        // Ending the room always stops future polling before any leave notification is sent.
        isInChannel = false;
        stopBanPolling();
        notifyMatchingLeave();
    }

    /**
     * Marks the current backend session as completed when the focus timer finishes.
     */
    void recordSessionComplete() {
        if (backendSessionId < 0) {
            // Skip completion calls when the backend session never started successfully.
            return;
        }
        SessionApi api = ApiClient.getSessionApi(appContext);
        api.completeSession(backendSessionId).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                // Completion acknowledgement is only logged because the UI has already moved on.
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
            // Early exits before a backend session exists have nothing to mark incomplete.
            return;
        }
        SessionApi api = ApiClient.getSessionApi(appContext);
        api.incompleteSession(backendSessionId).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                // Incomplete-session acknowledgement is also background bookkeeping only.
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
        // Clearing the previous id forces the next start call to register a brand-new backend session.
        backendSessionId = -1;
        // A new backend session is created for the next focus round.
        recordSessionStart();
    }

    /**
     * Polls the backend for restriction updates while the user remains in the meeting.
     */
    private void startBanPolling() {
        // Reset any previous polling loop before starting a new one for this room join.
        stopBanPolling();
        banCheckHandler = new Handler(Looper.getMainLooper());
        banCheckHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isInChannel) {
                    // Stop scheduling once the room has ended.
                    return;
                }
                checkBanStatus();
                if (banCheckHandler != null) {
                    // Keep the same runnable alive for the next cycle.
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
            // Removing all callbacks stops the repeating poll immediately.
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
                // The current profile is enough to detect new bans or temporary restrictions.
                UserProfileResponse profile = response.body();
                if (response.isSuccessful() && profile != null && profile.isCurrentlyRestricted()) {
                    // Once restricted, stop polling and let the activity decide how to remove the user.
                    stopBanPolling();
                    callbacks.onUserRestricted(profile);
                }
                // Non-restricted users simply stay in the room until the next poll.
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                // Failed polls are intentionally ignored because the next scheduled poll will retry.
                Log.d(TAG, "Ban check failed, retrying on next poll", t);
            }
        });
    }

    /**
     * Creates the backend session record used for completion tracking and task assignment.
     */
    private void recordSessionStart() {
        SessionApi api = ApiClient.getSessionApi(appContext);
        // The backend session ties the selected duration to the channel currently being used.
        StartSessionRequest request = new StartSessionRequest(focusDurationMinutes, channelName);
        api.startSession(request).enqueue(new Callback<StartSessionResponse>() {
            @Override
            public void onResponse(Call<StartSessionResponse> call,
                                   Response<StartSessionResponse> response) {
                StartSessionResponse body = response.body();
                if (response.isSuccessful() && body != null) {
                    // Store the backend id for later complete/incomplete calls.
                    backendSessionId = body.getSessionId();
                    Log.d(TAG, "Backend session started: " + backendSessionId);
                    // Task assignment only happens after the backend has created a concrete session id.
                    assignPendingTasks(backendSessionId);
                }
                // If the backend does not return a valid body, the room continues without session tracking.
            }

            @Override
            public void onFailure(Call<StartSessionResponse> call, Throwable t) {
                // Session tracking can fail independently from the Agora room itself.
                Log.w(TAG, "Failed to record session start", t);
            }
        });
    }

    /**
     * Associates any pending tasks with the current backend session.
     */
    private void assignPendingTasks(long sessionId) {
        TaskApi taskApi = ApiClient.getTaskApi(appContext);
        // Task assignment is best-effort and should not block the meeting from continuing.
        taskApi.assignTasksToSession(new AssignTasksRequest(sessionId))
                .enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        // Task assignment success is informational only and does not change room UI.
                        Log.d(TAG, "Tasks assigned to session " + sessionId);
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        // Task assignment failure is logged only because the session itself can still continue.
                        Log.w(TAG, "Failed to assign tasks", t);
                    }
                });
    }

    /**
     * Tells the matching service that this user has left the room.
     */
    private void notifyMatchingLeave() {
        if (matchingLeaveNotified || channelName == null || channelName.isEmpty()) {
            // Skip duplicate notifications and invalid channel names.
            return;
        }
        // Mark it early so repeated cleanup paths cannot send duplicates.
        matchingLeaveNotified = true;

        MatchingApi matchingApi = ApiClient.getMatchingApi(appContext);
        matchingApi.leaveMeeting(channelName).enqueue(new Callback<LeaveMeetingResponse>() {
            @Override
            public void onResponse(Call<LeaveMeetingResponse> call,
                                   Response<LeaveMeetingResponse> response) {
                // The room is already closing, so a log entry is enough here.
                Log.d(TAG, "Notified matching engine of leave");
            }

            @Override
            public void onFailure(Call<LeaveMeetingResponse> call, Throwable t) {
                // Leave notification failures are logged only because the room is already closing.
                Log.w(TAG, "Failed to notify matching engine of leave", t);
            }
        });
    }
}
