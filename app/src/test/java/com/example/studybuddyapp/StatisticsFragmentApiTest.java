package com.example.studybuddyapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.SessionApi;
import com.example.studybuddyapp.api.dto.SessionHistoryItem;
import com.example.studybuddyapp.api.dto.SessionStatistics;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class StatisticsFragmentApiTest {

    @Test
    public void onResume_populatesStatisticsAndHistoryFromApi() throws Exception {
        SessionApi sessionApi = mock(SessionApi.class);
        Call<SessionStatistics> statisticsCall = mock(Call.class);
        Call<java.util.List<SessionHistoryItem>> historyCall = mock(Call.class);

        SessionStatistics statistics = new SessionStatistics();
        setField(statistics, "rating", 4.7d);
        setField(statistics, "totalFocusTimeMinutes", 135L);

        SessionHistoryItem completedSession = new SessionHistoryItem();
        setField(completedSession, "durationMinutes", 30);
        setField(completedSession, "completed", true);
        setField(completedSession, "startedAt", "2026-03-27T10:15:30");

        SessionHistoryItem.TaskSummary completedTask = new SessionHistoryItem.TaskSummary();
        setField(completedTask, "title", "Finish chapter notes");
        setField(completedTask, "completed", true);
        setField(completedSession, "tasks", Collections.singletonList(completedTask));

        when(sessionApi.getStatistics()).thenReturn(statisticsCall);
        when(sessionApi.getHistory()).thenReturn(historyCall);
        doAnswer(invocation -> {
            Callback<SessionStatistics> callback = invocation.getArgument(0);
            callback.onResponse(statisticsCall, Response.success(statistics));
            return null;
        }).when(statisticsCall).enqueue(any());
        doAnswer(invocation -> {
            Callback<java.util.List<SessionHistoryItem>> callback = invocation.getArgument(0);
            callback.onResponse(historyCall, Response.success(Arrays.asList(completedSession)));
            return null;
        }).when(historyCall).enqueue(any());

        try (MockedStatic<ApiClient> apiClientMock = Mockito.mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.getSessionApi(any())).thenReturn(sessionApi);

            FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
            StatisticsFragment fragment = new StatisticsFragment();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, fragment)
                    .commitNow();

            View view = fragment.requireView();
            assertEquals("4.7", ((TextView) view.findViewById(R.id.tvRating)).getText().toString());
            assertEquals("2h 15m", ((TextView) view.findViewById(R.id.tvFocusTime)).getText().toString());

            LinearLayout historyContainer = view.findViewById(R.id.sessionHistoryContainer);
            assertTrue(historyContainer.getChildCount() >= 3);

            View sessionRow = historyContainer.getChildAt(0);
            assertEquals("Focus Duration: 30:00",
                    ((TextView) sessionRow.findViewById(R.id.tvSessionDuration)).getText().toString());
            assertEquals("Completed",
                    ((TextView) sessionRow.findViewById(R.id.tvSessionStatus)).getText().toString());
            assertEquals("Mar 27 - 2026",
                    ((TextView) sessionRow.findViewById(R.id.tvSessionDate)).getText().toString());

            TextView taskLine = (TextView) historyContainer.getChildAt(1);
            assertEquals("\u2713 Finish chapter notes", taskLine.getText().toString());
            assertFalse(view.findViewById(R.id.tvEmptyHistory).isShown());
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
