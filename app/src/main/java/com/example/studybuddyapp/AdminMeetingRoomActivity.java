package com.example.studybuddyapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AdminMeetingRoomActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_meeting_room);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        View.OnClickListener prohibitClickListener = v -> showAdminDecisionDialog();
        findViewById(R.id.prohibitBtn1).setOnClickListener(prohibitClickListener);
        findViewById(R.id.prohibitBtn2).setOnClickListener(prohibitClickListener);
        findViewById(R.id.prohibitBtn3).setOnClickListener(prohibitClickListener);
        findViewById(R.id.prohibitBtn4).setOnClickListener(prohibitClickListener);
        findViewById(R.id.prohibitBtn5).setOnClickListener(prohibitClickListener);
    }

    private void showAdminDecisionDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_admin_decision, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnKickOut).setOnClickListener(v -> {
            Toast.makeText(this, "User kicked out", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btnBan3Days).setOnClickListener(v -> {
            Toast.makeText(this, "User banned for 3 days", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btnBanPermanently).setOnClickListener(v -> {
            Toast.makeText(this, "User banned permanently", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btnBack).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
