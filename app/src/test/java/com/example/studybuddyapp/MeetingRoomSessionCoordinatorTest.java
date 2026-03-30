package com.example.studybuddyapp;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.MatchingApi;
import com.example.studybuddyapp.api.SessionApi;
import com.example.studybuddyapp.api.TaskApi;
import com.example.studybuddyapp.api.dto.AssignTasksRequest;
import com.example.studybuddyapp.api.dto.LeaveMeetingResponse;
import com.example.studybuddyapp.api.dto.StartSessionRequest;
import com.example.studybuddyapp.api.dto.StartSessionResponse;
import com.example.studybuddyapp.api.dto.UserProfileResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class MeetingRoomSessionCoordinatorTest {

    @Test
    public void onChannelJoined_startsSessionAndAssignsTasksToReturnedSessionId() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        SessionApi sessionApi = mock(SessionApi.class);
        TaskApi taskApi = mock(TaskApi.class);
        MatchingApi matchingApi = mock(MatchingApi.class);
        Call<StartSessionResponse> startSessionCall = mock(Call.class);
        Call<String> assignTasksCall = mock(Call.class);
        Call<LeaveMeetingResponse> leaveMeetingCall = mock(Call.class);
        StartSessionResponse startResponse = new StartSessionResponse();
        setField(startResponse, "sessionId", 88L);

        when(sessionApi.startSession(any(StartSessionRequest.class))).thenReturn(startSessionCall);
        when(taskApi.assignTasksToSession(any(AssignTasksRequest.class))).thenReturn(assignTasksCall);
        when(matchingApi.leaveMeeting("focus-room-30")).thenReturn(leaveMeetingCall);
        doAnswer(invocation -> {
            Callback<StartSessionResponse> callback = invocation.getArgument(0);
            callback.onResponse(startSessionCall, Response.success(startResponse));
            return null;
        }).when(startSessionCall).enqueue(any());
        doAnswer(invocation -> null).when(assignTasksCall).enqueue(any());
        doAnswer(invocation -> null).when(leaveMeetingCall).enqueue(any());

        try (MockedStatic<ApiClient> apiClientMock = Mockito.mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.getSessionApi(any())).thenReturn(sessionApi);
            apiClientMock.when(() -> ApiClient.getTaskApi(any())).thenReturn(taskApi);
            apiClientMock.when(() -> ApiClient.getMatchingApi(any())).thenReturn(matchingApi);

            MeetingRoomSessionCoordinator coordinator =
                    new MeetingRoomSessionCoordinator(context, "focus-room-30", 30, new NoOpCallbacks());

            coordinator.onChannelJoined();

            ArgumentCaptor<StartSessionRequest> startCaptor =
                    ArgumentCaptor.forClass(StartSessionRequest.class);
            verify(sessionApi).startSession(startCaptor.capture());
            assertEquals(30, startCaptor.getValue().getDurationMinutes());
            assertEquals("focus-room-30", startCaptor.getValue().getChannelName());

            ArgumentCaptor<AssignTasksRequest> assignCaptor =
                    ArgumentCaptor.forClass(AssignTasksRequest.class);
            verify(taskApi).assignTasksToSession(assignCaptor.capture());
            assertEquals(88L, assignCaptor.getValue().getSessionId());

            coordinator.onMeetingEnded();
        }
    }

    @Test
    public void onMeetingEnded_notifiesMatchingApiOnlyOnce() {
        Context context = ApplicationProvider.getApplicationContext();
        MatchingApi matchingApi = mock(MatchingApi.class);
        Call<LeaveMeetingResponse> leaveMeetingCall = mock(Call.class);

        when(matchingApi.leaveMeeting("focus-room-15")).thenReturn(leaveMeetingCall);
        doAnswer(invocation -> null).when(leaveMeetingCall).enqueue(any());

        try (MockedStatic<ApiClient> apiClientMock = Mockito.mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.getMatchingApi(any())).thenReturn(matchingApi);

            MeetingRoomSessionCoordinator coordinator =
                    new MeetingRoomSessionCoordinator(context, "focus-room-15", 15, new NoOpCallbacks());

            coordinator.onMeetingEnded();
            coordinator.onMeetingEnded();

            verify(matchingApi, times(1)).leaveMeeting("focus-room-15");
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class NoOpCallbacks implements MeetingRoomSessionCoordinator.Callbacks {
        @Override
        public boolean shouldHandleCallbacks() {
            return true;
        }

        @Override
        public void onUserRestricted(UserProfileResponse profile) {
        }
    }
}
