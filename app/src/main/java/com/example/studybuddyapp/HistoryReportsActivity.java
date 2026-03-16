package com.example.studybuddyapp;

import android.os.Bundle;
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
 * Activity responsible for displaying a history of all user reports.
 * This screen is restricted to Administrators and fulfills requirement [MOD-6],
 * allowing admins to review flagged accounts, the meeting context, and the reasons.
 */
public class HistoryReportsActivity extends AppCompatActivity {

    // The container where we will programmatically add the report items
    private LinearLayout reportsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history_reports);

        // Setup window insets for edge-to-edge UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Close activity on back arrow click
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());
        
        // Ensure your activity_history_reports.xml has a ScrollView wrapping a LinearLayout with id 'reportsContainer'
        reportsContainer = findViewById(R.id.reportsContainer); 
        
        // Fetch the reports from the backend as soon as the screen opens
        fetchReportsFromBackend();
    }

    /**
     * Makes a network call to the Moderation API to retrieve all reports.
     * If successful, it populates the UI with the retrieved data.
     */
    private void fetchReportsFromBackend() {
        ModerationApi api = ApiClient.getModerationApi(this);
        
        api.getAllReports().enqueue(new Callback<List<ReportResponse>>() {
            @Override
            public void onResponse(Call<List<ReportResponse>> call, Response<List<ReportResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    populateReportsList(response.body());
                } else {
                    Toast.makeText(HistoryReportsActivity.this, "Failed to load reports.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ReportResponse>> call, Throwable t) {
                Toast.makeText(HistoryReportsActivity.this, "Network error.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Iterates through the list of reports and dynamically creates UI elements
     * to display them on the screen.
     * * @param reports The list of reports retrieved from the database.
     */
    private void populateReportsList(List<ReportResponse> reports) {
        // Clear any existing views before populating
        if (reportsContainer != null) {
            reportsContainer.removeAllViews();
            
            for (ReportResponse report : reports) {
                // Create a new text view for each report
                TextView reportView = new TextView(this);
                
                // Format the string to show the reported user, meeting ID, and reason
                String reportText = "Reported User ID: " + report.getReportedUserId() + "\n"
                                  + "Meeting ID: " + report.getMeetingId() + "\n"
                                  + "Reason: " + report.getReason() + "\n"
                                  + "Status: " + report.getStatus() + "\n\n";
                                  
                reportView.setText(reportText);
                reportView.setTextSize(16f);
                reportView.setPadding(32, 32, 32, 32);
                
                // Add the dynamically created view to the layout container
                reportsContainer.addView(reportView);
            }
        }
    }
}