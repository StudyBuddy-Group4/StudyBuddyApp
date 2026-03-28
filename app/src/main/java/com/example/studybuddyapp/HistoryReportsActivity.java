package com.example.studybuddyapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.ModerationApi;
import com.example.studybuddyapp.api.dto.ReportResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Shows the admin's report history list.
 */
public class HistoryReportsActivity extends AppCompatActivity {

    // Admin header labels
    private TextView tvAdminName;
    private TextView tvAdminId;
    // List container
    private LinearLayout reportsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history_reports);

        // Apply system-bar padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Bind header and list views
        tvAdminName = findViewById(R.id.tvAdminName);
        tvAdminId = findViewById(R.id.tvAdminId);
        reportsContainer = findViewById(R.id.reportsContainer);

        // Back closes this screen
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        // Fill the header from session data
        displayAdminInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the report list on return
        fetchReportsFromBackend();
    }

    /**
     * Fills the admin header from the current session.
     */
    private void displayAdminInfo() {
        SessionManager session = new SessionManager(this);
        // Cached username
        String username = session.getUsername();
        // Cached user id
        long userId = session.getUserId();

        if (username != null && !username.isEmpty()) {
            tvAdminName.setText(username);
        }
        if (userId > 0) {
            tvAdminId.setText("ID: " + userId);
        }
    }

    /**
     * Loads the latest reports from the backend.
     */
    private void fetchReportsFromBackend() {
        ModerationApi api = ApiClient.getModerationApi(this);

        api.getAllReports().enqueue(new Callback<List<ReportResponse>>() {
            @Override
            public void onResponse(Call<List<ReportResponse>> call,
                                   Response<List<ReportResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Render the returned reports
                    populateReportsList(response.body());
                } else {
                    // Show a simple load failure message
                    Toast.makeText(HistoryReportsActivity.this,
                            "Failed to load reports.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ReportResponse>> call, Throwable t) {
                // Network error while loading reports
                Toast.makeText(HistoryReportsActivity.this,
                        "Network error.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Rebuilds the visible report list.
     */
    private void populateReportsList(List<ReportResponse> reports) {
        // Clear the old rows first
        reportsContainer.removeAllViews();

        if (reports.isEmpty()) {
            // Empty state
            TextView empty = new TextView(this);
            empty.setText("No reports found.");
            empty.setTextSize(16f);
            empty.setPadding(0, 32, 0, 32);
            empty.setTextColor(getColor(R.color.secondary_text));
            reportsContainer.addView(empty);
            return;
        }

        for (ReportResponse report : reports) {
            // Inflate one history row
            View row = LayoutInflater.from(this)
                    .inflate(R.layout.item_report_history, reportsContainer, false);

            ImageView ivIcon = row.findViewById(R.id.ivReportIcon);
            TextView tvUser = row.findViewById(R.id.tvReportUser);
            TextView tvAction = row.findViewById(R.id.tvReportAction);
            TextView tvDate = row.findViewById(R.id.tvReportDate);

            // Fallback when the backend user id is missing
            String userId = report.getReportedUserId() != null
                    ? String.valueOf(report.getReportedUserId()) : "?";
            tvUser.setText("Flag : " + userId);

            // Default to pending when the status is missing
            String status = report.getStatus() != null ? report.getStatus() : "PENDING";
            tvAction.setText(mapStatusToLabel(status));

            if ("DISMISSED".equals(status)) {
                // Dismissed reports use the close icon
                ivIcon.setImageResource(R.drawable.ic_close_circle);
            } else {
                // Other states use the warning icon
                ivIcon.setImageResource(R.drawable.ic_prohibited);
            }

            // Format the backend timestamp
            tvDate.setText(formatDate(report.getTimestamp()));

            // Add the row
            reportsContainer.addView(row);

            // Add a divider below each row
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
            divider.setBackgroundColor(getColor(R.color.input_field_bg));
            reportsContainer.addView(divider);
        }
    }

    /**
     * Maps backend report states to the shorter labels shown in the UI.
     */
    private String mapStatusToLabel(String status) {
        switch (status) {
            case "ACTIONED":
                // Report was handled by an admin
                return "Action taken";
            case "DISMISSED":
                // Report was rejected
                return "Invalid";
            case "PENDING":
                // Report still needs review
                return "Pending review";
            default:
                // Fallback to the raw backend value
                return status;
        }
    }

    /**
     * Converts an ISO date string into the report history display format.
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
     * Converts dp units into pixels for runtime-created views.
     */
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
