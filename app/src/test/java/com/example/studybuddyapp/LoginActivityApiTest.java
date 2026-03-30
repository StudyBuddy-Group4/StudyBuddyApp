package com.example.studybuddyapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.AuthApi;
import com.example.studybuddyapp.api.dto.LoginRequest;
import com.example.studybuddyapp.api.dto.LoginResponse;

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
public class LoginActivityApiTest {

    @Test
    public void loginSuccess_savesSessionAndNavigatesToMainHub() throws Exception {
        AuthApi authApi = mock(AuthApi.class);
        Call<LoginResponse> loginCall = mock(Call.class);
        LoginResponse loginResponse = new LoginResponse();
        setField(loginResponse, "token", "jwt-123");
        setField(loginResponse, "userId", 42L);
        setField(loginResponse, "username", "alice");
        setField(loginResponse, "isAdmin", true);

        when(authApi.login(any(LoginRequest.class))).thenReturn(loginCall);
        Mockito.doAnswer(invocation -> {
            Callback<LoginResponse> callback = invocation.getArgument(0);
            callback.onResponse(loginCall, Response.success(loginResponse));
            return null;
        }).when(loginCall).enqueue(any());

        try (MockedStatic<ApiClient> apiClientMock = Mockito.mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.getAuthApi(any())).thenReturn(authApi);

            LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();

            ((EditText) activity.findViewById(R.id.etEmail)).setText("alice@example.com");
            ((EditText) activity.findViewById(R.id.etPassword)).setText("secret");
            Button loginButton = activity.findViewById(R.id.btnLogIn);

            loginButton.performClick();

            ArgumentCaptor<LoginRequest> requestCaptor = ArgumentCaptor.forClass(LoginRequest.class);
            verify(authApi).login(requestCaptor.capture());
            assertEquals("alice@example.com", getField(requestCaptor.getValue(), "usernameOrEmail"));
            assertEquals("secret", getField(requestCaptor.getValue(), "password"));
            assertTrue(loginButton.isEnabled());

            SessionManager sessionManager = new SessionManager(activity);
            assertEquals("jwt-123", sessionManager.getToken());
            assertEquals(42L, sessionManager.getUserId());
            assertEquals("alice", sessionManager.getUsername());
            assertTrue(sessionManager.isAdmin());

            Intent nextIntent = shadowOf(activity).getNextStartedActivity();
            assertNotNull(nextIntent);
            assertEquals(MainHubActivity.class.getName(), nextIntent.getComponent().getClassName());
            assertTrue(nextIntent.getBooleanExtra(MainHubActivity.EXTRA_IS_ADMIN, false));
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
