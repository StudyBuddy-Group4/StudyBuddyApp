package com.example.studybuddyapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.SessionApi;
import com.example.studybuddyapp.api.dto.SessionHistoryItem;
import com.example.studybuddyapp.api.dto.SessionStatistics;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatisticsFragment extends Fragment {

    private TextView tvRating;
    private TextView tvFocusTime;
    private LinearLayout sessionHistoryContainer;
    private TextView tvEmptyHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvRating = view.findViewById(R.id.tvRating);
        tvFocusTime = view.findViewById(R.id.tvFocusTime);
        sessionHistoryContainer = view.findViewById(R.id.sessionHistoryContainer);
        tvEmptyHistory = view.findViewById(R.id.tvEmptyHistory);

        view.findViewById(R.id.ic_question).setOnClickListener(v -> showRatingExplanation());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStatistics();
        loadHistory();
    }

    private void loadStatistics() {
        SessionApi api = ApiClient.getSessionApi(requireContext());
        api.getStatistics().enqueue(new Callback<SessionStatistics>() {
            @Override
            public void onResponse(Call<SessionStatistics> call,
                                   Response<SessionStatistics> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    SessionStatistics stats = response.body();
                    tvRating.setText(String.format(Locale.US, "%.1f", stats.getRating()));

                    long totalMin = stats.getTotalFocusTimeMinutes();
                    long hours = totalMin / 60;
                    long mins = totalMin % 60;
                    tvFocusTime.setText(String.format(Locale.US, "%dh %dm", hours, mins));
                }
            }

            @Override
            public void onFailure(Call<SessionStatistics> call, Throwable t) {
                // Keep showing defaults
            }
        });
    }

    private void loadHistory() {
        SessionApi api = ApiClient.getSessionApi(requireContext());
        api.getHistory().enqueue(new Callback<List<SessionHistoryItem>>() {
            @Override
            public void onResponse(Call<List<SessionHistoryItem>> call,
                                   Response<List<SessionHistoryItem>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    populateHistory(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<SessionHistoryItem>> call, Throwable t) {
                // Keep showing empty
            }
        });
    }

    private void populateHistory(List<SessionHistoryItem> sessions) {
        sessionHistoryContainer.removeAllViews();

        if (sessions.isEmpty()) {
            tvEmptyHistory.setVisibility(View.VISIBLE);
            return;
        }
        tvEmptyHistory.setVisibility(View.GONE);

        for (SessionHistoryItem item : sessions) {
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_session_history, sessionHistoryContainer, false);

            ImageView icon = row.findViewById(R.id.ivStatusIcon);
            TextView tvDuration = row.findViewById(R.id.tvSessionDuration);
            TextView tvStatus = row.findViewById(R.id.tvSessionStatus);
            TextView tvDate = row.findViewById(R.id.tvSessionDate);

            boolean completed = Boolean.TRUE.equals(item.getCompleted());
            icon.setImageResource(completed ? R.drawable.ic_trophy : R.drawable.ic_close_circle);

            int mins = item.getDurationMinutes();
            tvDuration.setText(String.format(Locale.US, "Focus Duration: %d:00", mins));

            tvStatus.setText(completed ? "Completed" : "Uncompleted");
            tvStatus.setTextColor(requireContext().getColor(
                    completed ? R.color.primary_green : R.color.red_hangup));

            String dateStr = formatDate(item.getStartedAt());
            tvDate.setText(dateStr);

            sessionHistoryContainer.addView(row);

            View divider = new View(requireContext());
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
            divider.setBackgroundColor(requireContext().getColor(R.color.input_field_bg));
            sessionHistoryContainer.addView(divider);
        }
    }

    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.length() < 10) return "";
        try {
            String[] parts = isoDate.substring(0, 10).split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            return months[month - 1] + " " + day + " - " + year;
        } catch (Exception e) {
            return isoDate.substring(0, 10);
        }
    }

    private void showRatingExplanation() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.how_rating_calculated)
                .setMessage(R.string.rating_explanation)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
