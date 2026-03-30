package com.example.studybuddyapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.UserApi;
import com.example.studybuddyapp.api.dto.UserProfileResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ProfileFragmentTest {

    @Test
    public void onResume_loadsProfileAndUpdatesSession() throws Exception {
        UserApi userApi = mock(UserApi.class);
        Call<UserProfileResponse> getProfileCall = mock(Call.class);
        UserProfileResponse profile = new UserProfileResponse();
        setField(profile, "id", 77L);
        setField(profile, "username", "backend-user");

        when(userApi.getProfile()).thenReturn(getProfileCall);
        Mockito.doAnswer(invocation -> {
            Callback<UserProfileResponse> callback = invocation.getArgument(0);
            callback.onResponse(getProfileCall, Response.success(profile));
            return null;
        }).when(getProfileCall).enqueue(any());

        try (MockedStatic<ApiClient> apiClientMock = Mockito.mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.getUserApi(any())).thenReturn(userApi);

            FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
            SessionManager sessionManager = new SessionManager(activity);
            sessionManager.saveLoginSession("jwt", 1L, "cached-name", false);

            ProfileFragment fragment = new ProfileFragment();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, fragment)
                    .commitNow();

            assertEquals("backend-user",
                    ((TextView) fragment.requireView().findViewById(R.id.tvProfileName)).getText().toString());
            assertEquals("ID: 77",
                    ((TextView) fragment.requireView().findViewById(R.id.tvProfileId)).getText().toString());
            assertEquals("backend-user", sessionManager.getUsername());
            assertEquals(77L, sessionManager.getUserId());
        }
    }

    @Test
    public void onViewCreated_showsCachedSessionDataBeforeBackendRefresh() {
        UserApi userApi = mock(UserApi.class);
        Call<UserProfileResponse> getProfileCall = mock(Call.class);

        when(userApi.getProfile()).thenReturn(getProfileCall);
        Mockito.doAnswer(invocation -> {
            Callback<UserProfileResponse> callback = invocation.getArgument(0);
            callback.onFailure(getProfileCall, new RuntimeException("offline"));
            return null;
        }).when(getProfileCall).enqueue(any());

        try (MockedStatic<ApiClient> apiClientMock = Mockito.mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.getUserApi(any())).thenReturn(userApi);

            FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
            SessionManager sessionManager = new SessionManager(activity);
            sessionManager.saveLoginSession("jwt", 22L, "alice", false);

            ProfileFragment fragment = new ProfileFragment();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, fragment)
                    .commitNow();

            assertNotNull(fragment.requireView());
            assertEquals("alice",
                    ((TextView) fragment.requireView().findViewById(R.id.tvProfileName)).getText().toString());
            assertEquals("ID: 22",
                    ((TextView) fragment.requireView().findViewById(R.id.tvProfileId)).getText().toString());
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
