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

/**
 * Displays the user's focus statistics and session history.
 */
public class StatisticsFragment extends Fragment {

    // Rating label
    private TextView tvRating;
    // Total focus time label
    private TextView tvFocusTime;
    // History rows
    private LinearLayout sessionHistoryContainer;
    // Empty-history label
    private TextView tvEmptyHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the statistics layout
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind summary and history views
        tvRating = view.findViewById(R.id.tvRating);
        tvFocusTime = view.findViewById(R.id.tvFocusTime);
        sessionHistoryContainer = view.findViewById(R.id.sessionHistoryContainer);
        tvEmptyHistory = view.findViewById(R.id.tvEmptyHistory);

        // Show the rating help dialog
        view.findViewById(R.id.ic_question).setOnClickListener(v -> showRatingExplanation());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh summary data
        loadStatistics();
        // Refresh history data
        loadHistory();
    }

    /**
     * Loads the current rating and total focus time from the backend.
     */
    private void loadStatistics() {
        SessionApi api = ApiClient.getSessionApi(requireContext());
        api.getStatistics().enqueue(new Callback<SessionStatistics>() {
            @Override
            public void onResponse(Call<SessionStatistics> call,
                                   Response<SessionStatistics> response) {
                // Ignore late callbacks
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    SessionStatistics stats = response.body();
                    // Show the backend rating
                    tvRating.setText(String.format(Locale.US, "%.1f", stats.getRating()));

                    // Split total minutes into hours and minutes
                    long totalMin = stats.getTotalFocusTimeMinutes();
                    long hours = totalMin / 60;
                    long mins = totalMin % 60;
                    tvFocusTime.setText(String.format(Locale.US, "%dh %dm", hours, mins));
                }
            }

            @Override
            public void onFailure(Call<SessionStatistics> call, Throwable t) {
                // Keep the current summary values
                // Keep showing defaults
            }
        });
    }

    /**
     * Loads the user's previous sessions and displays them in the history section.
     */
    private void loadHistory() {
        SessionApi api = ApiClient.getSessionApi(requireContext());
        api.getHistory().enqueue(new Callback<List<SessionHistoryItem>>() {
            @Override
            public void onResponse(Call<List<SessionHistoryItem>> call,
                                   Response<List<SessionHistoryItem>> response) {
                // Ignore late callbacks
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    // Render the returned history items
                    populateHistory(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<SessionHistoryItem>> call, Throwable t) {
                // Keep the current history state
                // Keep showing empty
            }
        });
    }

    /**
     * Renders each session row and any completed tasks that belong to that session.
     */
    private void populateHistory(List<SessionHistoryItem> sessions) {
        // Clear old rows first
        sessionHistoryContainer.removeAllViews();

        if (sessions.isEmpty()) {
            // Empty-history state
            tvEmptyHistory.setVisibility(View.VISIBLE);
            return;
        }
        tvEmptyHistory.setVisibility(View.GONE);

        for (SessionHistoryItem item : sessions) {
            // Inflate one session row
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_session_history, sessionHistoryContainer, false);

            ImageView icon = row.findViewById(R.id.ivStatusIcon);
            TextView tvDuration = row.findViewById(R.id.tvSessionDuration);
            TextView tvStatus = row.findViewById(R.id.tvSessionStatus);
            TextView tvDate = row.findViewById(R.id.tvSessionDate);

            // Trophy for completed, close icon otherwise
            boolean completed = Boolean.TRUE.equals(item.getCompleted());
            icon.setImageResource(completed ? R.drawable.ic_trophy : R.drawable.ic_close_circle);

            // Show the session duration in minutes
            int mins = item.getDurationMinutes();
            tvDuration.setText(String.format(Locale.US, "Focus Duration: %d:00", mins));

            // Show status text and color
            tvStatus.setText(completed ? "Completed" : "Uncompleted");
            tvStatus.setTextColor(requireContext().getColor(
                    completed ? R.color.primary_green : R.color.red_hangup));

            // Format the backend start date
            String dateStr = formatDate(item.getStartedAt());
            tvDate.setText(dateStr);

            // Add the main session row
            sessionHistoryContainer.addView(row);

            if (item.getTasks() != null && !item.getTasks().isEmpty()) {
                for (SessionHistoryItem.TaskSummary task : item.getTasks()) {
                    // Only completed tasks are shown below the session
                    if (!Boolean.TRUE.equals(task.getCompleted())) continue;
                    TextView taskLine = new TextView(requireContext());
                    // Prefix completed tasks with a check mark
                    taskLine.setText("\u2713 " + task.getTitle());
                    taskLine.setTextSize(13);
                    taskLine.setPadding(dpToPx(40), dpToPx(2), 0, dpToPx(2));
                    taskLine.setTextColor(requireContext().getColor(R.color.primary_green));
                    // Add the task line under the session row
                    sessionHistoryContainer.addView(taskLine);
                }
            }

            // Divider between sessions
            View divider = new View(requireContext());
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
            divider.setBackgroundColor(requireContext().getColor(R.color.input_field_bg));
            sessionHistoryContainer.addView(divider);
        }
    }

    /**
     * Converts an ISO date string into the format shown in the history list.
     */
    private String formatDate(String isoDate) {
        // Missing or short values cannot be formatted
        if (isoDate == null || isoDate.length() < 10) return "";
        try {
            // Use only the date part
            String[] parts = isoDate.substring(0, 10).split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            return months[month - 1] + " " + day + " - " + year;
        } catch (Exception e) {
            // Fall back to the raw date slice
            return isoDate.substring(0, 10);
        }
    }

    /**
     * Shows a short explanation of how the rating is calculated.
     */
    private void showRatingExplanation() {
        // Reuse a simple alert dialog for the rating help text
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.how_rating_calculated)
                .setMessage(R.string.rating_explanation)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * Converts density-independent pixels to actual pixels for runtime views.
     */
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
