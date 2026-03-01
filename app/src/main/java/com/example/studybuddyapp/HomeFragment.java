package com.example.studybuddyapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    private TextView chip15, chip30, chipCustom;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chip15 = view.findViewById(R.id.chip_15_min);
        chip30 = view.findViewById(R.id.chip_30_min);
        chipCustom = view.findViewById(R.id.chip_custom);
        Button btnStart = view.findViewById(R.id.btn_start);

        chip15.setOnClickListener(v -> selectChip(chip15));
        chip30.setOnClickListener(v -> selectChip(chip30));
        chipCustom.setOnClickListener(v -> selectChip(chipCustom));

        btnStart.setOnClickListener(v -> showReadyToFocusDialog());
    }

    private void selectChip(TextView selected) {
        TextView[] chips = {chip15, chip30, chipCustom};
        for (TextView chip : chips) {
            if (chip == selected) {
                chip.setBackgroundResource(R.drawable.bg_time_chip_selected);
                chip.setTextColor(getResources().getColor(R.color.white, null));
            } else {
                chip.setBackgroundResource(R.drawable.bg_time_chip);
                chip.setTextColor(getResources().getColor(R.color.dark_text, null));
            }
        }
    }

    private void showReadyToFocusDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_ready_to_focus, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btn_add_task).setOnClickListener(v -> {
            dialog.dismiss();
            if (getActivity() instanceof MainHubActivity) {
                ((MainHubActivity) getActivity()).switchToTasksTab();
            }
        });

        dialogView.findViewById(R.id.btn_start_anyway).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(requireContext(), MeetingRoomActivity.class));
        });

        dialog.show();
    }
}
