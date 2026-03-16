package com.example.studybuddyapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.ErrorUtils;
import com.example.studybuddyapp.api.MatchingApi;
import com.example.studybuddyapp.api.dto.JoinMeetingResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private TextView chip15, chip30, chipCustom;
    private int selectedDurationMinutes = 15;
    private boolean isCustomSelected = false;

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

        chip15.setOnClickListener(v -> {
            selectedDurationMinutes = 15;
            isCustomSelected = false;
            chipCustom.setText(getString(R.string.time_custom));
            selectChip(chip15);
        });

        chip30.setOnClickListener(v -> {
            selectedDurationMinutes = 30;
            isCustomSelected = false;
            chipCustom.setText(getString(R.string.time_custom));
            selectChip(chip30);
        });

        chipCustom.setOnClickListener(v -> showCustomMinutesDialog());

        btnStart.setOnClickListener(v -> {
            if (selectedDurationMinutes <= 0) {
                Toast.makeText(requireContext(),
                        "Please enter a valid number of minutes.", Toast.LENGTH_SHORT).show();
                return;
            }
            showReadyToFocusDialog();
        });
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

    private void showCustomMinutesDialog() {
        final EditText input = new EditText(requireContext());
        input.setHint("Enter minutes");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setPadding(50, 30, 50, 30);

        new AlertDialog.Builder(requireContext())
                .setTitle("Custom focus time")
                .setMessage("Enter the number of minutes:")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "Please enter a number.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int minutes = Integer.parseInt(value);
                    if (minutes <= 0) {
                        Toast.makeText(requireContext(),
                                "Minutes must be greater than 0.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    selectedDurationMinutes = minutes;
                    isCustomSelected = true;
                    chipCustom.setText(minutes + " min");
                    selectChip(chipCustom);
                })
                .setNegativeButton("Cancel", null)
                .show();
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

            MatchingApi api = ApiClient.getMatchingApi(getContext());
            api.join(selectedDurationMinutes).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<JoinMeetingResponse> call, Response<JoinMeetingResponse> response) {
                    JoinMeetingResponse body = response.body();
                    if (response.isSuccessful() && response.body() != null) {
                        Intent intent = new Intent(requireContext(), MeetingRoomActivity.class);
                        intent.putExtra(MeetingRoomActivity.EXTRA_CHANNEL_NAME, body.getChannelName());
                        intent.putExtra(MeetingRoomActivity.EXTRA_FOCUS_DURATION, selectedDurationMinutes);
                        startActivity(intent);
                    } else {
                        String msg = ErrorUtils.parseError(response,
                                "Matching failed");
                        Toast.makeText(getContext(), msg,
                                Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<JoinMeetingResponse> call, Throwable throwable) {
                    Toast.makeText(getContext(),
                            "Network error. Is the backend running?", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }
}
