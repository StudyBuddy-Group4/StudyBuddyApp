package com.example.studybuddyapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

public class TaskListFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.fab_add_task).setOnClickListener(v -> showCreateTaskDialog());
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

        dialogView.findViewById(R.id.btn_dialog_back).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btn_dialog_save).setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(requireContext(), "Task saved!", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
}
