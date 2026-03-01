package com.example.studybuddyapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SecurityCodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_security_code);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView ivBack = findViewById(R.id.ivBack);
        Button btnConfirm = findViewById(R.id.btnConfirm);
        Button btnSendAgain = findViewById(R.id.btnSendAgain);

        EditText etCode1 = findViewById(R.id.etCode1);
        EditText etCode2 = findViewById(R.id.etCode2);
        EditText etCode3 = findViewById(R.id.etCode3);
        EditText etCode4 = findViewById(R.id.etCode4);
        EditText etCode5 = findViewById(R.id.etCode5);
        EditText etCode6 = findViewById(R.id.etCode6);

        etCode1.setText("2");
        etCode2.setText("7");
        etCode3.setText("3");
        etCode4.setText("9");
        etCode5.setText("1");
        etCode6.setText("6");

        ivBack.setOnClickListener(v -> finish());

        btnConfirm.setOnClickListener(v ->
                startActivity(new Intent(this, NewPasswordActivity.class)));

        btnSendAgain.setOnClickListener(v -> { /* No-op for mock */ });
    }
}
