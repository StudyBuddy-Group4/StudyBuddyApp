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

import com.example.studybuddyapp.api.dto.TaskItem;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class TaskListFragmentInstrumentedTest {

    private LaunchOptionsActivity activity;
    private TaskListFragment fragment;

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

            fragment = new TaskListFragment();
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
    public void renderTasks_showsEmptyStateWhenTaskListIsEmpty() throws Exception {
        invokeRenderTasks();

        TextView emptyView = fragment.requireView().findViewById(R.id.tvEmptyTasks);
        LinearLayout taskContainer = fragment.requireView().findViewById(R.id.taskContainer);

        assertEquals(View.VISIBLE, emptyView.getVisibility());
        assertEquals(0, taskContainer.getChildCount());
    }

    @Test
    public void renderTasks_showsTaskNoteWhenPresent() throws Exception {
        addTask(createTaskItem(1L, "Read chapter", "Finish section 2", false));

        invokeRenderTasks();

        LinearLayout taskContainer = fragment.requireView().findViewById(R.id.taskContainer);
        TextView emptyView = fragment.requireView().findViewById(R.id.tvEmptyTasks);
        View row = taskContainer.getChildAt(0);
        TextView titleView = row.findViewById(R.id.tvTaskTitle);
        TextView noteView = row.findViewById(R.id.tvTaskNote);
        ImageView deleteView = row.findViewById(R.id.ivDeleteTask);

        assertEquals("Read chapter", titleView.getText().toString());
        assertEquals("Note: Finish section 2", noteView.getText().toString());
        assertEquals(View.VISIBLE, noteView.getVisibility());
        assertEquals(View.VISIBLE, deleteView.getVisibility());
        assertEquals(View.GONE, emptyView.getVisibility());
    }

    @Test
    public void renderTasks_hidesTaskNoteAndShowsCompletedIconState() throws Exception {
        addTask(createTaskItem(2L, "Submit draft", "", true));

        invokeRenderTasks();

        LinearLayout taskContainer = fragment.requireView().findViewById(R.id.taskContainer);
        View row = taskContainer.getChildAt(0);
        TextView noteView = row.findViewById(R.id.tvTaskNote);
        ImageView statusView = row.findViewById(R.id.ivTaskStatus);

        assertEquals(View.GONE, noteView.getVisibility());
        assertTrue(drawablesMatch(statusView.getDrawable(), R.drawable.ic_check_circle));
        assertFalse(drawablesMatch(statusView.getDrawable(), R.drawable.ic_unchecked_circle));
    }

    @Test
    public void renderTasks_showsUncheckedIconForIncompleteTask() throws Exception {
        addTask(createTaskItem(5L, "Prepare slides", null, false));

        invokeRenderTasks();

        LinearLayout taskContainer = fragment.requireView().findViewById(R.id.taskContainer);
        View row = taskContainer.getChildAt(0);
        ImageView statusView = row.findViewById(R.id.ivTaskStatus);

        assertTrue(drawablesMatch(statusView.getDrawable(), R.drawable.ic_unchecked_circle));
        assertFalse(drawablesMatch(statusView.getDrawable(), R.drawable.ic_check_circle));
    }

    @Test
    public void renderTasks_addsDividerBetweenMultipleTasks() throws Exception {
        addTask(createTaskItem(3L, "Task one", null, false));
        addTask(createTaskItem(4L, "Task two", null, false));

        invokeRenderTasks();

        LinearLayout taskContainer = fragment.requireView().findViewById(R.id.taskContainer);

        assertEquals(3, taskContainer.getChildCount());
        assertNotNull(taskContainer.getChildAt(1));
        assertEquals(View.class, taskContainer.getChildAt(1).getClass());
    }

    private void invokeRenderTasks() throws Exception {
        Method renderTasks = TaskListFragment.class.getDeclaredMethod("renderTasks");
        renderTasks.setAccessible(true);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                renderTasks.invoke(fragment);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    @SuppressWarnings("unchecked")
    private void addTask(TaskItem task) throws Exception {
        Field currentTasksField = TaskListFragment.class.getDeclaredField("currentTasks");
        currentTasksField.setAccessible(true);
        List<TaskItem> currentTasks = (List<TaskItem>) currentTasksField.get(fragment);
        currentTasks.add(task);
    }

    private TaskItem createTaskItem(long id, String title, String note, boolean completed)
            throws Exception {
        TaskItem taskItem = new TaskItem();
        setField(taskItem, "id", id);
        setField(taskItem, "title", title);
        setField(taskItem, "note", note);
        setField(taskItem, "completed", completed);
        return taskItem;
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
