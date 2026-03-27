package com.example.studybuddyapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.studybuddyapp.api.dto.SessionHistoryItem;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class StatisticsFragmentInstrumentedTest {

    private LaunchOptionsActivity activity;
    private StatisticsFragment fragment;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, LaunchOptionsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        activity = (LaunchOptionsActivity) InstrumentationRegistry.getInstrumentation()
                .startActivitySync(intent);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            FrameLayout container = new FrameLayout(activity);
            container.setId(View.generateViewId());
            activity.setContentView(container);

            fragment = new StatisticsFragment();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(container.getId(), fragment)
                    .commitNow();
        });
    }

    @After
    public void tearDown() {
        if (activity != null) {
            activity.finish();
        }
    }

    @Test
    public void populateHistory_showsEmptyStateWhenNoSessionsExist() throws Exception {
        invokePopulateHistory(Collections.emptyList());

        TextView emptyView = fragment.requireView().findViewById(R.id.tvEmptyHistory);
        LinearLayout container = fragment.requireView().findViewById(R.id.sessionHistoryContainer);

        assertEquals(View.VISIBLE, emptyView.getVisibility());
        assertEquals(0, container.getChildCount());
    }

    @Test
    public void populateHistory_rendersCompletedSessionDetails() throws Exception {
        List<SessionHistoryItem> sessions = Collections.singletonList(
                createSessionHistoryItem(1L, 45, true, "2026-03-27T10:15:30", null)
        );

        invokePopulateHistory(sessions);

        LinearLayout container = fragment.requireView().findViewById(R.id.sessionHistoryContainer);
        TextView emptyView = fragment.requireView().findViewById(R.id.tvEmptyHistory);
        View row = container.getChildAt(0);
        TextView duration = row.findViewById(R.id.tvSessionDuration);
        TextView status = row.findViewById(R.id.tvSessionStatus);
        TextView date = row.findViewById(R.id.tvSessionDate);

        assertEquals(View.GONE, emptyView.getVisibility());
        assertEquals("Focus Duration: 45:00", duration.getText().toString());
        assertEquals("Completed", status.getText().toString());
        assertEquals("Mar 27 - 2026", date.getText().toString());
    }

    @Test
    public void populateHistory_rendersUncompletedSessionWithFailureColor() throws Exception {
        List<SessionHistoryItem> sessions = Collections.singletonList(
                createSessionHistoryItem(2L, 30, false, "2026-03-25T08:00:00", null)
        );

        invokePopulateHistory(sessions);

        LinearLayout container = fragment.requireView().findViewById(R.id.sessionHistoryContainer);
        View row = container.getChildAt(0);
        ImageView icon = row.findViewById(R.id.ivStatusIcon);
        TextView status = row.findViewById(R.id.tvSessionStatus);

        assertEquals("Uncompleted", status.getText().toString());
        assertEquals(fragment.requireContext().getColor(R.color.red_hangup), status.getCurrentTextColor());
        assertTrue(drawablesMatch(icon.getDrawable(), R.drawable.ic_close_circle));
        assertFalse(drawablesMatch(icon.getDrawable(), R.drawable.ic_trophy));
    }

    @Test
    public void populateHistory_showsOnlyCompletedTasksBelowSession() throws Exception {
        List<SessionHistoryItem.TaskSummary> tasks = new ArrayList<>();
        tasks.add(createTaskSummary(11L, "Review notes", true));
        tasks.add(createTaskSummary(12L, "Ignored task", false));

        List<SessionHistoryItem> sessions = Collections.singletonList(
                createSessionHistoryItem(3L, 20, true, "2026-03-20T12:00:00", tasks)
        );

        invokePopulateHistory(sessions);

        LinearLayout container = fragment.requireView().findViewById(R.id.sessionHistoryContainer);

        assertEquals(3, container.getChildCount());
        assertEquals("\u2713 Review notes", ((TextView) container.getChildAt(1)).getText().toString());
    }

    private void invokePopulateHistory(List<SessionHistoryItem> sessions) throws Exception {
        Method populateHistory = StatisticsFragment.class
                .getDeclaredMethod("populateHistory", List.class);
        populateHistory.setAccessible(true);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                populateHistory.invoke(fragment, sessions);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private SessionHistoryItem createSessionHistoryItem(long id,
                                                        int durationMinutes,
                                                        boolean completed,
                                                        String startedAt,
                                                        List<SessionHistoryItem.TaskSummary> tasks)
            throws Exception {
        SessionHistoryItem item = new SessionHistoryItem();
        setField(item, "id", id);
        setField(item, "durationMinutes", durationMinutes);
        setField(item, "completed", completed);
        setField(item, "startedAt", startedAt);
        setField(item, "tasks", tasks);
        return item;
    }

    private SessionHistoryItem.TaskSummary createTaskSummary(long id,
                                                             String title,
                                                             boolean completed)
            throws Exception {
        SessionHistoryItem.TaskSummary task = new SessionHistoryItem.TaskSummary();
        setField(task, "id", id);
        setField(task, "title", title);
        setField(task, "completed", completed);
        return task;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private boolean drawablesMatch(Drawable actual, int expectedResId) {
        Drawable expected = fragment.requireContext().getDrawable(expectedResId);
        return actual != null
                && expected != null
                && drawableToBitmap(actual).sameAs(drawableToBitmap(expected));
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        int width = Math.max(1, drawable.getIntrinsicWidth());
        int height = Math.max(1, drawable.getIntrinsicHeight());

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
