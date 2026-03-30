package com.example.studybuddyapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.widget.EditText;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.UserApi;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ChangePasswordActivityApiTest {

    @Test
    public void changePasswordSuccess_sendsExpectedBodyAndNavigates() {
        UserApi userApi = mock(UserApi.class);
        Call<String> changeCall = mock(Call.class);

        when(userApi.changePassword(any())).thenReturn(changeCall);
        Mockito.doAnswer(invocation -> {
            Callback<String> callback = invocation.getArgument(0);
            callback.onResponse(changeCall, Response.success("ok"));
            return null;
        }).when(changeCall).enqueue(any());

        try (MockedStatic<ApiClient> apiClientMock = Mockito.mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.getUserApi(any())).thenReturn(userApi);

            ChangePasswordActivity activity =
                    Robolectric.buildActivity(ChangePasswordActivity.class).setup().get();

            ((EditText) activity.findViewById(R.id.etCurrentPassword)).setText("old-pass");
            ((EditText) activity.findViewById(R.id.etNewPassword)).setText("new-pass");
            ((EditText) activity.findViewById(R.id.etConfirmPassword)).setText("new-pass");
            activity.findViewById(R.id.btnChangePassword).performClick();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, String>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
            verify(userApi).changePassword(bodyCaptor.capture());
            assertEquals("old-pass", bodyCaptor.getValue().get("currentPassword"));
            assertEquals("new-pass", bodyCaptor.getValue().get("newPassword"));

            Intent nextIntent = shadowOf(activity).getNextStartedActivity();
            assertNotNull(nextIntent);
            assertEquals(PasswordChangedSuccessActivity.class.getName(),
                    nextIntent.getComponent().getClassName());
        }
    }

    @Test
    public void samePasswordAsCurrent_doesNotCallApi() {
        UserApi userApi = mock(UserApi.class);

        try (MockedStatic<ApiClient> apiClientMock = Mockito.mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.getUserApi(any())).thenReturn(userApi);

            ChangePasswordActivity activity =
                    Robolectric.buildActivity(ChangePasswordActivity.class).setup().get();

            ((EditText) activity.findViewById(R.id.etCurrentPassword)).setText("same-pass");
            ((EditText) activity.findViewById(R.id.etNewPassword)).setText("same-pass");
            ((EditText) activity.findViewById(R.id.etConfirmPassword)).setText("same-pass");
            activity.findViewById(R.id.btnChangePassword).performClick();

            verify(userApi, never()).changePassword(any());
        }
    }
}
