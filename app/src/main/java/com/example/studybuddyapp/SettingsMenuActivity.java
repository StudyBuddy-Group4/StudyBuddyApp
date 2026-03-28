package com.example.studybuddyapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Hosts the general settings menu.
 */
public class SettingsMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings_menu);

        // Apply system-bar padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Back closes this screen
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        // Open notification settings
        findViewById(R.id.menuNotificationSettings).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationSettingsActivity.class)));

        // Open delete-account flow
        findViewById(R.id.menuDeleteAccount).setOnClickListener(v ->
                startActivity(new Intent(this, DeleteAccountActivity.class)));
    }
}
