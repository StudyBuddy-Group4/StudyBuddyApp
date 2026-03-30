package com.example.studybuddyapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * First screen in the password-recovery flow.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        // This screen starts the mocked password-recovery flow used in the app.
        // Apply system-bar padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // The back arrow and next-step button are the only actions on this page.
        // Screen actions
        ImageView ivBack = findViewById(R.id.ivBack);
        Button btnNextStep = findViewById(R.id.btnNextStep);

        // Leaving here keeps the user in the auth flow instead of opening the app.
        // Back returns to the previous screen
        ivBack.setOnClickListener(v -> finish());

        // The next step opens the mock verification-code screen.
        // Continue to the security-code step
        btnNextStep.setOnClickListener(v ->
                startActivity(new Intent(this, SecurityCodeActivity.class)));
    }
}
