package com.example.studybuddyapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class FlagParticipantActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_flag_participant);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        View.OnClickListener flagClickListener = v -> showFlagReasonDialog();
        findViewById(R.id.flagBtn1).setOnClickListener(flagClickListener);
        findViewById(R.id.flagBtn2).setOnClickListener(flagClickListener);
        findViewById(R.id.flagBtn3).setOnClickListener(flagClickListener);
        findViewById(R.id.flagBtn4).setOnClickListener(flagClickListener);
    }

    private void showFlagReasonDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_flag_reason, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            Toast.makeText(this, "Flag submitted", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            finish();
        });

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
