package com.example.studybuddyapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Dialog;
import android.widget.Button;
import android.widget.EditText;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.AuthApi;
import com.example.studybuddyapp.api.dto.RegisterRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import java.lang.reflect.Field;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class CreateAccountActivityApiTest {

    @Test
    public void signUpSuccess_showsDialogAndNavigatesBackToLogin() throws Exception {
        AuthApi authApi = mock(AuthApi.class);
        Call<String> registerCall = mock(Call.class);

        when(authApi.register(any(RegisterRequest.class))).thenReturn(registerCall);
        Mockito.doAnswer(invocation -> {
            Callback<String> callback = invocation.getArgument(0);
            callback.onResponse(registerCall, Response.success("created"));
            return null;
        }).when(registerCall).enqueue(any());

        try (MockedStatic<ApiClient> apiClientMock = Mockito.mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.getAuthApi(any())).thenReturn(authApi);

            CreateAccountActivity activity =
                    Robolectric.buildActivity(CreateAccountActivity.class).setup().get();

            ((EditText) activity.findViewById(R.id.etUsername)).setText("new-user");
            ((EditText) activity.findViewById(R.id.etEmail)).setText("new@example.com");
            ((EditText) activity.findViewById(R.id.etPassword)).setText("secret");
            ((EditText) activity.findViewById(R.id.etConfirmPassword)).setText("secret");

            Button signUpButton = activity.findViewById(R.id.btnSignUp);
            signUpButton.performClick();

            ArgumentCaptor<RegisterRequest> requestCaptor =
                    ArgumentCaptor.forClass(RegisterRequest.class);
            verify(authApi).register(requestCaptor.capture());
            assertEquals("new-user", getField(requestCaptor.getValue(), "username"));
            assertEquals("new@example.com", getField(requestCaptor.getValue(), "email"));
            assertEquals("secret", getField(requestCaptor.getValue(), "password"));

            Dialog dialog = ShadowDialog.getLatestDialog();
            assertNotNull(dialog);
            Button positiveButton = dialog.findViewById(android.R.id.button1);
            assertNotNull(positiveButton);
            assertTrue(signUpButton.isEnabled());
        }
    }

    @Test
    public void signUpWithMismatchedPasswords_doesNotCallApi() {
        AuthApi authApi = mock(AuthApi.class);

        try (MockedStatic<ApiClient> apiClientMock = Mockito.mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.getAuthApi(any())).thenReturn(authApi);

            CreateAccountActivity activity =
                    Robolectric.buildActivity(CreateAccountActivity.class).setup().get();

            ((EditText) activity.findViewById(R.id.etUsername)).setText("new-user");
            ((EditText) activity.findViewById(R.id.etEmail)).setText("new@example.com");
            ((EditText) activity.findViewById(R.id.etPassword)).setText("secret");
            ((EditText) activity.findViewById(R.id.etConfirmPassword)).setText("different");

            ((Button) activity.findViewById(R.id.btnSignUp)).performClick();

            verify(authApi, never()).register(any(RegisterRequest.class));
        }
    }

    private static Object getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
