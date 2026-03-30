package com.example.studybuddyapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.MatchingApi;
import com.example.studybuddyapp.api.UserApi;
import com.example.studybuddyapp.api.dto.JoinMeetingResponse;
import com.example.studybuddyapp.api.dto.UserProfileResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class HomeFragmentTest {

    @Test
    public void checkUserStatusAndLaunch_unrestrictedUserStartsMeetingRoom() throws Exception {
        UserApi userApi = mock(UserApi.class);
        MatchingApi matchingApi = mock(MatchingApi.class);
        Call<UserProfileResponse> profileCall = mock(Call.class);
        Call<JoinMeetingResponse> joinMeetingCall = mock(Call.class);
        UserProfileResponse profile = new UserProfileResponse();
        JoinMeetingResponse joinResponse = new JoinMeetingResponse();

        setField(profile, "isBanned", false);
        setField(profile, "bannedUntil", null);
        setField(joinResponse, "channelName", "focus-15-room");

        when(userApi.getProfile()).thenReturn(profileCall);
        when(matchingApi.joinMeeting(15)).thenReturn(joinMeetingCall);
        Mockito.doAnswer(invocation -> {
            Callback<UserProfileResponse> callback = invocation.getArgument(0);
            callback.onResponse(profileCall, Response.success(profile));
            return null;
        }).when(profileCall).enqueue(any());
        Mockito.doAnswer(invocation -> {
            Callback<JoinMeetingResponse> callback = invocation.getArgument(0);
            callback.onResponse(joinMeetingCall, Response.success(joinResponse));
            return null;
        }).when(joinMeetingCall).enqueue(any());

        try (MockedStatic<ApiClient> apiClientMock = Mockito.mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.getUserApi(any())).thenReturn(userApi);
            apiClientMock.when(() -> ApiClient.getMatchingApi(any())).thenReturn(matchingApi);

            FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
            HomeFragment fragment = new HomeFragment();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, fragment)
                    .commitNow();

            Method method = HomeFragment.class.getDeclaredMethod("checkUserStatusAndLaunch");
            method.setAccessible(true);
            method.invoke(fragment);

            Intent nextIntent = shadowOf(activity).getNextStartedActivity();
            assertNotNull(nextIntent);
            assertEquals(MeetingRoomActivity.class.getName(), nextIntent.getComponent().getClassName());
            assertEquals(15, nextIntent.getIntExtra(MeetingRoomActivity.EXTRA_FOCUS_DURATION, -1));
            assertEquals("focus-15-room",
                    nextIntent.getStringExtra(MeetingRoomActivity.EXTRA_CHANNEL_NAME));
        }
    }

    @Test
    public void checkUserStatusAndLaunch_restrictedUserShowsRestrictionDialog() throws Exception {
        UserApi userApi = mock(UserApi.class);
        Call<UserProfileResponse> profileCall = mock(Call.class);
        UserProfileResponse profile = new UserProfileResponse();

        setField(profile, "isBanned", true);
        when(userApi.getProfile()).thenReturn(profileCall);
        Mockito.doAnswer(invocation -> {
            Callback<UserProfileResponse> callback = invocation.getArgument(0);
            callback.onResponse(profileCall, Response.success(profile));
            return null;
        }).when(profileCall).enqueue(any());

        try (MockedStatic<ApiClient> apiClientMock = Mockito.mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.getUserApi(any())).thenReturn(userApi);

            FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
            HomeFragment fragment = new HomeFragment();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, fragment)
                    .commitNow();

            Method method = HomeFragment.class.getDeclaredMethod("checkUserStatusAndLaunch");
            method.setAccessible(true);
            method.invoke(fragment);

            AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
            assertNotNull(dialog);
            TextView messageView = dialog.findViewById(R.id.tvWarningMessage);
            assertNotNull(messageView);
            assertTrue(messageView.getText().toString().contains("permanently banned"));
        }
    }

    @Test
    public void launchMeetingRoom_unsuccessfulResponse_doesNotStartActivity() throws Exception {
        MatchingApi matchingApi = mock(MatchingApi.class);
        Call<JoinMeetingResponse> joinMeetingCall = mock(Call.class);

        when(matchingApi.joinMeeting(15)).thenReturn(joinMeetingCall);
        Mockito.doAnswer(invocation -> {
            Callback<JoinMeetingResponse> callback = invocation.getArgument(0);
            callback.onResponse(joinMeetingCall, Response.error(500,
                    okhttp3.ResponseBody.create(null, "server error")));
            return null;
        }).when(joinMeetingCall).enqueue(any());

        try (MockedStatic<ApiClient> apiClientMock = Mockito.mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.getMatchingApi(any())).thenReturn(matchingApi);

            FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
            HomeFragment fragment = new HomeFragment();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, fragment)
                    .commitNow();

            Method method = HomeFragment.class.getDeclaredMethod("launchMeetingRoom");
            method.setAccessible(true);
            method.invoke(fragment);

            assertNull(shadowOf(activity).getNextStartedActivity());
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
