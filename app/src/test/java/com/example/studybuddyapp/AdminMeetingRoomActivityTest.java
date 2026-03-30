package com.example.studybuddyapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.ModerationApi;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AdminMeetingRoomActivityTest {

    @Test
    public void addParticipant_ignoresDuplicateUid() throws Exception {
        AdminMeetingRoomActivity activity = buildActivity();

        Method addParticipant = AdminMeetingRoomActivity.class.getDeclaredMethod("addParticipant", int.class);
        addParticipant.setAccessible(true);
        addParticipant.invoke(activity, 12);
        addParticipant.invoke(activity, 12);

        @SuppressWarnings("unchecked")
        List<Integer> remoteUids = (List<Integer>) getField(activity, "remoteUids");
        LinearLayout grid = activity.findViewById(R.id.participantGrid);

        assertEquals(1, remoteUids.size());
        assertEquals(1, grid.getChildCount());
    }

    @Test
    public void createParticipantCell_showsUidLabel() throws Exception {
        AdminMeetingRoomActivity activity = buildActivity();
        Method createParticipantCell = AdminMeetingRoomActivity.class
                .getDeclaredMethod("createParticipantCell", int.class);
        createParticipantCell.setAccessible(true);

        View cell = (View) createParticipantCell.invoke(activity, 33);
        TextView uidLabel = findTextViewWithText(cell, "ID: 33");

        assertNotNull(uidLabel);
        assertEquals("ID: 33", uidLabel.getText().toString());
    }

    @Test
    public void executeAdminAction_successAlsoMarksReportActioned() throws Exception {
        ModerationApi moderationApi = mock(ModerationApi.class);
        Call<Void> applyActionCall = mock(Call.class);
        Call<Void> updateStatusCall = mock(Call.class);

        when(moderationApi.applyAdminAction(44, "KICK")).thenReturn(applyActionCall);
        when(moderationApi.updateReportStatus(9L, "ACTIONED")).thenReturn(updateStatusCall);
        Mockito.doAnswer(invocation -> {
            Callback<Void> callback = invocation.getArgument(0);
            callback.onResponse(applyActionCall, Response.success(null));
            return null;
        }).when(applyActionCall).enqueue(any());
        Mockito.doAnswer(invocation -> {
            Callback<Void> callback = invocation.getArgument(0);
            callback.onResponse(updateStatusCall, Response.success(null));
            return null;
        }).when(updateStatusCall).enqueue(any());

        try (MockedStatic<ApiClient> apiClientMock = Mockito.mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.getModerationApi(any())).thenReturn(moderationApi);

            AdminMeetingRoomActivity activity = buildActivity();
            setField(activity, "reportId", 9L);

            Method executeAdminAction = AdminMeetingRoomActivity.class
                    .getDeclaredMethod("executeAdminAction", int.class, String.class);
            executeAdminAction.setAccessible(true);
            executeAdminAction.invoke(activity, 44, "KICK");

            verify(moderationApi).applyAdminAction(44, "KICK");
            verify(moderationApi).updateReportStatus(9L, "ACTIONED");
        }
    }

    private static AdminMeetingRoomActivity buildActivity() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.putExtra("channel_name", "admin-room");
        intent.putExtra("report_id", 5L);
        intent.putExtra("reported_user_id", 8L);
        return Robolectric.buildActivity(AdminMeetingRoomActivity.class, intent).setup().get();
    }

    private static TextView findTextViewWithText(View root, String expectedText) {
        if (root instanceof TextView && expectedText.equals(((TextView) root).getText().toString())) {
            return (TextView) root;
        }
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView match = findTextViewWithText(group.getChildAt(i), expectedText);
                if (match != null) return match;
            }
        }
        return null;
    }

    private static Object getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
