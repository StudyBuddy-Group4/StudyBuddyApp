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

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnLogIn = findViewById(R.id.btnLogIn);
        TextView tvSignUp = findViewById(R.id.tvSignUp);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogIn.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainHubActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        tvSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, CreateAccountActivity.class)));

        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }
}
