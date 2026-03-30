package com.example.studybuddyapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.TaskApi;
import com.example.studybuddyapp.api.dto.TaskItem;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TaskListFragmentApiTest {

    @Test
    public void onResume_loadsTasksAndToggleCompletionUpdatesTaskState() throws Exception {
        TaskApi taskApi = mock(TaskApi.class);
        Call<List<TaskItem>> pendingTasksCall = mock(Call.class);
        Call<String> markCompleteCall = mock(Call.class);
        TaskItem task = buildTask(7L, "Read summary", "Chapter 3", false);

        when(taskApi.getPendingTasks()).thenReturn(pendingTasksCall);
        when(taskApi.markComplete(7L)).thenReturn(markCompleteCall);
        doAnswer(invocation -> {
            Callback<List<TaskItem>> callback = invocation.getArgument(0);
            callback.onResponse(pendingTasksCall, Response.success(Collections.singletonList(task)));
            return null;
        }).when(pendingTasksCall).enqueue(any());
        doAnswer(invocation -> {
            Callback<String> callback = invocation.getArgument(0);
            callback.onResponse(markCompleteCall, Response.success("ok"));
            return null;
        }).when(markCompleteCall).enqueue(any());

        try (MockedStatic<ApiClient> apiClientMock = Mockito.mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.getTaskApi(any())).thenReturn(taskApi);

            FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
            TaskListFragment fragment = new TaskListFragment();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, fragment)
                    .commitNow();

            View fragmentView = fragment.requireView();
            LinearLayout taskContainer = fragmentView.findViewById(R.id.taskContainer);
            assertTrue(taskContainer.getChildCount() >= 1);

            View row = taskContainer.getChildAt(0);
            assertEquals("Read summary",
                    ((TextView) row.findViewById(R.id.tvTaskTitle)).getText().toString());
            assertEquals("Note: Chapter 3",
                    ((TextView) row.findViewById(R.id.tvTaskNote)).getText().toString());
            assertFalse(fragmentView.findViewById(R.id.tvEmptyTasks).isShown());

            ImageView statusView = row.findViewById(R.id.ivTaskStatus);
            statusView.performClick();

            verify(taskApi).markComplete(7L);
            assertTrue(Boolean.TRUE.equals(task.getCompleted()));
        }
    }

    @Test
    public void deleteTaskSuccess_removesRowFromRenderedList() throws Exception {
        TaskApi taskApi = mock(TaskApi.class);
        Call<List<TaskItem>> pendingTasksCall = mock(Call.class);
        Call<String> deleteCall = mock(Call.class);
        TaskItem task = buildTask(12L, "Solve exercises", "", false);

        when(taskApi.getPendingTasks()).thenReturn(pendingTasksCall);
        when(taskApi.deleteTask(12L)).thenReturn(deleteCall);
        doAnswer(invocation -> {
            Callback<List<TaskItem>> callback = invocation.getArgument(0);
            callback.onResponse(pendingTasksCall, Response.success(Collections.singletonList(task)));
            return null;
        }).when(pendingTasksCall).enqueue(any());
        doAnswer(invocation -> {
            Callback<String> callback = invocation.getArgument(0);
            callback.onResponse(deleteCall, Response.success("deleted"));
            return null;
        }).when(deleteCall).enqueue(any());

        try (MockedStatic<ApiClient> apiClientMock = Mockito.mockStatic(ApiClient.class)) {
            apiClientMock.when(() -> ApiClient.getTaskApi(any())).thenReturn(taskApi);

            FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
            TaskListFragment fragment = new TaskListFragment();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, fragment)
                    .commitNow();

            View fragmentView = fragment.requireView();
            LinearLayout taskContainer = fragmentView.findViewById(R.id.taskContainer);
            assertEquals(1, taskContainer.getChildCount());

            View row = taskContainer.getChildAt(0);
            row.findViewById(R.id.ivDeleteTask).performClick();

            verify(taskApi).deleteTask(12L);
            assertEquals(0, taskContainer.getChildCount());
            assertEquals(View.VISIBLE, fragmentView.findViewById(R.id.tvEmptyTasks).getVisibility());
        }
    }

    private static TaskItem buildTask(long id, String title, String note, boolean completed) throws Exception {
        TaskItem task = new TaskItem();
        setField(task, "id", id);
        setField(task, "title", title);
        setField(task, "note", note);
        setField(task, "completed", completed);
        return task;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
