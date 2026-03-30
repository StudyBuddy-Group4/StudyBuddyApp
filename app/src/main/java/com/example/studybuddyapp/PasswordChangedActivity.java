package com.example.studybuddyapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Short transition screen shown after a password change.
 */
public class PasswordChangedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_password_changed);

        // This page is only a short bridge between entering a new password and logging in again.
        // Apply system-bar padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Wait briefly before returning to login
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // The user does not need to interact here; the screen forwards automatically.
            Intent intent = new Intent(PasswordChangedActivity.this, LoginActivity.class);
            // Clear the back stack after a successful password change
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // Login becomes the new root after the password flow finishes.
            startActivity(intent);
        }, 2000);
    }
}
