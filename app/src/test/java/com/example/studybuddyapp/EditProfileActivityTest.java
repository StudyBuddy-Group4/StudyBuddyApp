package com.example.studybuddyapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.widget.EditText;
import android.widget.TextView;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.UserApi;
import com.example.studybuddyapp.api.dto.UpdateProfileRequest;
import com.example.studybuddyapp.api.dto.UserProfileResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
public class EditProfileActivityTest {

    @Test
    public void loadProfileFailure_fallsBackToCachedSessionData() {
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

            EditProfileActivity activity =
                    Robolectric.buildActivity(EditProfileActivity.class).get();
            SessionManager sessionManager = new SessionManager(activity);
            sessionManager.saveLoginSession("jwt", 55L, "cached-user", false);

            activity = Robolectric.buildActivity(EditProfileActivity.class).setup().get();

            assertEquals("cached-user",
                    ((EditText) activity.findViewById(R.id.etUsername)).getText().toString());
            assertEquals("cached-user",
                    ((TextView) activity.findViewById(R.id.tvHeaderName)).getText().toString());
        }
    }

    @Test
    public void updateProfileSuccess_updatesSessionAndHeader() throws Exception {
        UserApi userApi = mock(UserApi.class);
        Call<UserProfileResponse> getProfileCall = mock(Call.class);
        Call<String> updateProfileCall = mock(Call.class);
        UserProfileResponse profile = new UserProfileResponse();
        setField(profile, "id", 10L);
        setField(profile, "username", "old-name");
        setField(profile, "email", "old@example.com");

        when(userApi.getProfile()).thenReturn(getProfileCall);
        when(userApi.updateProfile(any(UpdateProfileRequest.class))).thenReturn(updateProfileCall);
        Mockito.doAnswer(invocation -> {
            Callback<UserProfileResponse> callback = invocation.getArgument(0);
            callback.onResponse(getProfileCall, Response.success(profile));
            return null;
        }).when(getProfileCall).enqueue(any());
        Mockito.doAnswer(invocation -> {
            Callback<String> callback = invocation.getArgument(0);
            callback.onResponse(updateProfileCall, Response.success("updated"));
            return null;
        }).when(updateProfileCall).enqueue(any());

        try (MockedStatic<ApiClient> apiClientMock = Mockito.mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.getUserApi(any())).thenReturn(userApi);

            EditProfileActivity activity =
                    Robolectric.buildActivity(EditProfileActivity.class).setup().get();
            SessionManager sessionManager = new SessionManager(activity);
            sessionManager.saveLoginSession("jwt", 10L, "old-name", false);

            ((EditText) activity.findViewById(R.id.etUsername)).setText("new-name");
            ((EditText) activity.findViewById(R.id.etEmail)).setText("new@example.com");
            activity.findViewById(R.id.btnUpdateProfile).performClick();

            ArgumentCaptor<UpdateProfileRequest> captor =
                    ArgumentCaptor.forClass(UpdateProfileRequest.class);
            verify(userApi).updateProfile(captor.capture());
            assertEquals("new-name", getField(captor.getValue(), "username"));
            assertEquals("new@example.com", getField(captor.getValue(), "email"));

            assertEquals("new-name",
                    ((TextView) activity.findViewById(R.id.tvHeaderName)).getText().toString());
            assertEquals("new-name", sessionManager.getUsername());
            assertTrue(activity.isFinishing());
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
