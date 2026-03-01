package com.example.studybuddyapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CreateAccountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_account);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView ivBack = findViewById(R.id.ivBack);
        Button btnSignUp = findViewById(R.id.btnSignUp);
        TextView tvLogIn = findViewById(R.id.tvLogIn);

        ivBack.setOnClickListener(v -> finish());

        btnSignUp.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.congratulations)
                    .setMessage(R.string.account_created_msg)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        });

        tvLogIn.setOnClickListener(v -> finish());
    }
}
