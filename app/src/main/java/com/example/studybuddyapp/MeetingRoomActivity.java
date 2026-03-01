package com.example.studybuddyapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MeetingRoomActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_meeting_room);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView btnHangUp = findViewById(R.id.btnHangUp);
        ImageView btnFlag = findViewById(R.id.btnFlag);

        btnHangUp.setOnClickListener(v -> showManualExitWarningDialog());

        btnFlag.setOnClickListener(v ->
                startActivity(new Intent(this, FlagParticipantActivity.class)));

        new Handler(Looper.getMainLooper()).postDelayed(this::showSessionCompletedDialog, 3000);
    }

    private void showManualExitWarningDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.meeting_room_title)
                .setMessage(R.string.manual_exit_warning)
                .setPositiveButton(R.string.btn_continue_studying, (d, w) -> d.dismiss())
                .setNegativeButton(R.string.btn_leave_session, (d, w) -> finish())
                .setCancelable(true)
                .create();
        dialog.show();
    }

    private void showSessionCompletedDialog() {
        if (isFinishing() || isDestroyed()) return;

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(R.string.session_congratulations)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_home, (d, w) -> {
                    Intent intent = new Intent(this, MainHubActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.btn_start_new_session, (d, w) -> d.dismiss())
                .setNeutralButton(R.string.btn_go_to_task_list, (d, w) -> {
                    Intent intent = new Intent(this, MainHubActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("navigate_to_tasks", true);
                    startActivity(intent);
                })
                .create();
        dialog.show();
    }
}
