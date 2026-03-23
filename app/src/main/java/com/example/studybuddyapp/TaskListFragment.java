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

public class TaskListFragment extends Fragment {

    private LinearLayout taskContainer;
    private TextView tvEmptyTasks;
    private final List<TaskItem> currentTasks = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        taskContainer = view.findViewById(R.id.taskContainer);
        tvEmptyTasks = view.findViewById(R.id.tvEmptyTasks);

        view.findViewById(R.id.fab_add_task).setOnClickListener(v -> showCreateTaskDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTasks();
    }

    private void loadTasks() {
        TaskApi api = ApiClient.getTaskApi(requireContext());
        api.getPendingTasks().enqueue(new Callback<List<TaskItem>>() {
            @Override
            public void onResponse(Call<List<TaskItem>> c, Response<List<TaskItem>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    currentTasks.clear();
                    currentTasks.addAll(response.body());
                    renderTasks();
                }
            }

            @Override
            public void onFailure(Call<List<TaskItem>> c, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(),
                        "Failed to load tasks", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderTasks() {
        taskContainer.removeAllViews();

        if (currentTasks.isEmpty()) {
            tvEmptyTasks.setVisibility(View.VISIBLE);
            return;
        }
        tvEmptyTasks.setVisibility(View.GONE);

        for (int i = 0; i < currentTasks.size(); i++) {
            TaskItem task = currentTasks.get(i);

            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_task, taskContainer, false);

            ImageView ivStatus = row.findViewById(R.id.ivTaskStatus);
            TextView tvTitle = row.findViewById(R.id.tvTaskTitle);
            TextView tvNote = row.findViewById(R.id.tvTaskNote);
            ImageView ivDelete = row.findViewById(R.id.ivDeleteTask);

            tvTitle.setText(task.getTitle());

            if (task.getNote() != null && !task.getNote().isEmpty()) {
                tvNote.setText("Note: " + task.getNote());
                tvNote.setVisibility(View.VISIBLE);
            }

            boolean completed = Boolean.TRUE.equals(task.getCompleted());
            ivStatus.setImageResource(completed
                    ? R.drawable.ic_check_circle : R.drawable.ic_unchecked_circle);

            ivStatus.setOnClickListener(v -> toggleTaskCompletion(task, ivStatus));

            ivDelete.setVisibility(View.VISIBLE);
            ivDelete.setOnClickListener(v -> deleteTask(task));

            taskContainer.addView(row);

            if (i < currentTasks.size() - 1) {
                View divider = new View(requireContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
                divider.setBackgroundColor(requireContext().getColor(R.color.input_field_bg));
                taskContainer.addView(divider);
            }
        }
    }

    private void toggleTaskCompletion(TaskItem task, ImageView ivStatus) {
        TaskApi api = ApiClient.getTaskApi(requireContext());
        boolean currentlyCompleted = Boolean.TRUE.equals(task.getCompleted());

        Call<String> call = currentlyCompleted
                ? api.markIncomplete(task.getId())
                : api.markComplete(task.getId());

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> c, Response<String> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    boolean newState = !currentlyCompleted;
                    task.setCompleted(newState);
                    ivStatus.setImageResource(newState
                            ? R.drawable.ic_check_circle : R.drawable.ic_unchecked_circle);
                }
            }

            @Override
            public void onFailure(Call<String> c, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(),
                        "Failed to update task", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteTask(TaskItem task) {
        TaskApi api = ApiClient.getTaskApi(requireContext());
        api.deleteTask(task.getId()).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> c, Response<String> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    currentTasks.remove(task);
                    renderTasks();
                }
            }

            @Override
            public void onFailure(Call<String> c, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(),
                        "Failed to delete task", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreateTaskDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_task, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etTask = dialogView.findViewById(R.id.et_task);
        EditText etNote = dialogView.findViewById(R.id.et_note);

        dialogView.findViewById(R.id.btn_dialog_back).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btn_dialog_save).setOnClickListener(v -> {
            String title = etTask.getText().toString().trim();
            String note = etNote.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(requireContext(),
                        "Please enter a task name.", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            createTask(title, note);
        });

        dialog.show();
    }

    private void createTask(String title, String note) {
        TaskApi api = ApiClient.getTaskApi(requireContext());
        api.createTask(new CreateTaskRequest(title, note.isEmpty() ? null : note))
                .enqueue(new Callback<TaskItem>() {
                    @Override
                    public void onResponse(Call<TaskItem> c, Response<TaskItem> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(),
                                    "Task created!", Toast.LENGTH_SHORT).show();
                            loadTasks();
                        }
                    }

                    @Override
                    public void onFailure(Call<TaskItem> c, Throwable t) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(),
                                "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
