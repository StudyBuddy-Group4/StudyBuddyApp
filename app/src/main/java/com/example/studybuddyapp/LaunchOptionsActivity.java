package com.example.studybuddyapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Entry screen that routes the user to login, sign-up, or password recovery.
 */
public class LaunchOptionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_launch_options);

        // Apply system-bar padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Launch options
        Button btnLogIn = findViewById(R.id.btnLogIn);
        Button btnSignUp = findViewById(R.id.btnSignUp);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Open login
        btnLogIn.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));

        // Open sign-up
        btnSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, CreateAccountActivity.class)));

        // Open password recovery
        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }
}
