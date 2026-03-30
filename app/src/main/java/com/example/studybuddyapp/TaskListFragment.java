package com.example.studybuddyapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.TaskApi;
import com.example.studybuddyapp.api.dto.CreateTaskRequest;
import com.example.studybuddyapp.api.dto.TaskItem;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Displays the user's pending tasks and allows basic task management actions.
 */
public class TaskListFragment extends Fragment {

    // The container is rebuilt from scratch every time the task list is re-rendered.
    private LinearLayout taskContainer;
    private TextView tvEmptyTasks;
    // Keep a local copy of the latest task data so the UI can be refreshed after edits and deletes.
    private final List<TaskItem> currentTasks = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // The task tab is rebuilt from XML each time because rows are added dynamically later.
        return inflater.inflate(R.layout.fragment_task_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind the list container and empty-state label from the fragment layout.
        taskContainer = view.findViewById(R.id.taskContainer);
        tvEmptyTasks = view.findViewById(R.id.tvEmptyTasks);

        // The floating action button is the only entry point for creating a task from this tab.
        view.findViewById(R.id.fab_add_task).setOnClickListener(v -> showCreateTaskDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload when returning to this tab so the list reflects the latest backend state.
        loadTasks();
    }

    /**
     * Loads the latest pending tasks from the backend whenever the fragment becomes active.
     */
    private void loadTasks() {
        // Pending tasks are the only ones shown in this fragment.
        TaskApi api = ApiClient.getTaskApi(requireContext());
        // Enqueue the api request for loading the tasks.
        api.getPendingTasks().enqueue(new Callback<List<TaskItem>>() {
            @Override
            public void onResponse(Call<List<TaskItem>> c, Response<List<TaskItem>> response) {
                // When fragment is not displayed.
                if (!isAdded()) return;
                // Otherwise.
                if (response.isSuccessful() && response.body() != null) {
                    // Replace the old in-memory list before rebuilding the UI.
                    currentTasks.clear();
                    currentTasks.addAll(response.body());
                    renderTasks();
                }
                // Unsuccessful responses leave the last rendered task list intact.
            }

            @Override
            public void onFailure(Call<List<TaskItem>> c, Throwable t) {
                if (!isAdded()) return;
                // Keep the old UI in place and simply notify the user about the failed refresh.
                Toast.makeText(requireContext(),
                        "Failed to load tasks", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Rebuilds the task list from the current in-memory task data.
     */
    private void renderTasks() {
        // Recreate the list from scratch to keep runtime-created rows in sync with currentTasks.
        taskContainer.removeAllViews();

        // Show the empty-state message when there are no tasks to render.
        if (currentTasks.isEmpty()) {
            tvEmptyTasks.setVisibility(View.VISIBLE);
            return;
        }
        tvEmptyTasks.setVisibility(View.GONE);

        for (int i = 0; i < currentTasks.size(); i++) {
            TaskItem task = currentTasks.get(i);

            // Every task row is inflated on demand from the shared item layout.
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_task, taskContainer, false);

            ImageView ivStatus = row.findViewById(R.id.ivTaskStatus);
            TextView tvTitle = row.findViewById(R.id.tvTaskTitle);
            TextView tvNote = row.findViewById(R.id.tvTaskNote);
            ImageView ivDelete = row.findViewById(R.id.ivDeleteTask);

            // The row title is always shown.
            tvTitle.setText(task.getTitle());

            // Only show the note field when the task actually has extra text.
            if (task.getNote() != null && !task.getNote().isEmpty()) {
                tvNote.setText("Note: " + task.getNote());
                tvNote.setVisibility(View.VISIBLE);
            }

            boolean completed = Boolean.TRUE.equals(task.getCompleted());
            // The stored completion flag controls both icon choice and later toggle direction.
            // The status icon reflects the last completion state known to the fragment.
            ivStatus.setImageResource(completed
                    ? R.drawable.ic_check_circle : R.drawable.ic_unchecked_circle);

            // Tapping the status icon toggles completion without leaving the list screen.
            ivStatus.setOnClickListener(v -> toggleTaskCompletion(task, ivStatus));

            ivDelete.setVisibility(View.VISIBLE);
            // Delete stays available for every pending task row.
            ivDelete.setOnClickListener(v -> deleteTask(task));

            // Add the fully configured row before optionally appending its divider.
            taskContainer.addView(row);

            // Add a divider between rows so multiple tasks stay visually separated.
            if (i < currentTasks.size() - 1) {
                View divider = new View(requireContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
                divider.setBackgroundColor(requireContext().getColor(R.color.input_field_bg));
                taskContainer.addView(divider);
            }
        }
    }

    /**
     * Toggles a task between complete and incomplete after the backend confirms the change.
     */
    private void toggleTaskCompletion(TaskItem task, ImageView ivStatus) {
        TaskApi api = ApiClient.getTaskApi(requireContext());
        boolean currentlyCompleted = Boolean.TRUE.equals(task.getCompleted());

        // Choose the matching backend endpoint based on the task's current completion state.
        Call<String> call = currentlyCompleted
                ? api.markIncomplete(task.getId())
                : api.markComplete(task.getId());

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> c, Response<String> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    // Keep the local task state and icon in sync with the confirmed backend result.
                    boolean newState = !currentlyCompleted;
                    task.setCompleted(newState);
                    ivStatus.setImageResource(newState
                            ? R.drawable.ic_check_circle : R.drawable.ic_unchecked_circle);
                }
                // Failed HTTP responses are ignored here so the last confirmed local state remains visible.
            }

            @Override
            public void onFailure(Call<String> c, Throwable t) {
                if (!isAdded()) return;
                // Failed updates leave the previous icon and local state unchanged.
                Toast.makeText(requireContext(),
                        "Failed to update task", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Deletes a task and refreshes the rendered list when the backend accepts the request.
     */
    private void deleteTask(TaskItem task) {
        TaskApi api = ApiClient.getTaskApi(requireContext());
        api.deleteTask(task.getId()).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> c, Response<String> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    // Remove the deleted task locally so the list updates immediately.
                    currentTasks.remove(task);
                    renderTasks();
                }
                // Unsuccessful delete responses keep the existing row visible.
            }

            @Override
            public void onFailure(Call<String> c, Throwable t) {
                if (!isAdded()) return;
                // On failure the row stays visible because the backend did not confirm deletion.
                Toast.makeText(requireContext(),
                        "Failed to delete task", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Opens the create-task dialog and validates the required task title field.
     */
    private void showCreateTaskDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_task, null);

        // Use a custom dialog layout so task title and note can be entered together.
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            // The dialog layout includes its own rounded background styling.
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etTask = dialogView.findViewById(R.id.et_task);
        EditText etNote = dialogView.findViewById(R.id.et_note);
        // Back simply closes the dialog without creating anything.
        dialogView.findViewById(R.id.btn_dialog_back).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btn_dialog_save).setOnClickListener(v -> {
            // Read both fields only when the user confirms the dialog.
            String title = etTask.getText().toString().trim();
            String note = etNote.getText().toString().trim();

            // A task without a title is not useful, so stop here before making a request.
            if (title.isEmpty()) {
                Toast.makeText(requireContext(),
                        "Please enter a task name.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Close the dialog before creating the task so the list can refresh underneath it.
            dialog.dismiss();
            createTask(title, note);
        });

        // Show the dialog only after both button listeners and fields have been wired up.
        dialog.show();
    }

    /**
     * Creates a new task and reloads the list so the screen reflects the backend state.
     */
    private void createTask(String title, String note) {
        TaskApi api = ApiClient.getTaskApi(requireContext());
        // Empty notes are converted to null so the request matches the backend DTO shape.
        api.createTask(new CreateTaskRequest(title, note.isEmpty() ? null : note))
                .enqueue(new Callback<TaskItem>() {
                    @Override
                    public void onResponse(Call<TaskItem> c, Response<TaskItem> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(),
                                    "Task created!", Toast.LENGTH_SHORT).show();
                            // Reload from the backend so the new task appears in the same order as the server.
                            loadTasks();
                        }
                        // Unsuccessful responses leave the current list unchanged.
                    }

                    @Override
                    public void onFailure(Call<TaskItem> c, Throwable t) {
                        if (!isAdded()) return;
                        // Creation failures keep the fragment state untouched so the user can try again.
                        Toast.makeText(requireContext(),
                                "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Converts density-independent pixels to actual pixels for runtime-created views.
     */
    private int dpToPx(int dp) {
        // Runtime dividers use this helper instead of hard-coded pixel values.
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
