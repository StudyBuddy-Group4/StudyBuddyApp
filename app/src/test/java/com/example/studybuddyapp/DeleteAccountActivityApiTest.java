package com.example.studybuddyapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.widget.EditText;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.UserApi;
import com.example.studybuddyapp.api.dto.DeleteAccountRequest;

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
public class DeleteAccountActivityApiTest {

    @Test
    public void deleteSuccess_clearsSessionAndNavigatesToLaunchOptions() throws Exception {
        UserApi userApi = mock(UserApi.class);
        Call<String> deleteCall = mock(Call.class);

        when(userApi.deleteAccount(any(DeleteAccountRequest.class))).thenReturn(deleteCall);
        Mockito.doAnswer(invocation -> {
            Callback<String> callback = invocation.getArgument(0);
            callback.onResponse(deleteCall, Response.success("deleted"));
            return null;
        }).when(deleteCall).enqueue(any());

        try (MockedStatic<ApiClient> apiClientMock = Mockito.mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.getUserApi(any())).thenReturn(userApi);
            apiClientMock.when(ApiClient::resetInstance).thenAnswer(invocation -> null);

            DeleteAccountActivity activity =
                    Robolectric.buildActivity(DeleteAccountActivity.class).setup().get();
            SessionManager sessionManager = new SessionManager(activity);
            sessionManager.saveLoginSession("jwt-1", 10L, "alice", false);

            ((EditText) activity.findViewById(R.id.etPassword)).setText("secret");
            activity.findViewById(R.id.btnDeleteAccount).performClick();

            ArgumentCaptor<DeleteAccountRequest> requestCaptor =
                    ArgumentCaptor.forClass(DeleteAccountRequest.class);
            verify(userApi).deleteAccount(requestCaptor.capture());
            assertEquals("secret", getField(requestCaptor.getValue(), "password"));
            apiClientMock.verify(ApiClient::resetInstance);

            assertFalse(sessionManager.isLoggedIn());

            Intent nextIntent = shadowOf(activity).getNextStartedActivity();
            assertNotNull(nextIntent);
            assertEquals(LaunchOptionsActivity.class.getName(), nextIntent.getComponent().getClassName());
        }
    }

    private static Object getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
