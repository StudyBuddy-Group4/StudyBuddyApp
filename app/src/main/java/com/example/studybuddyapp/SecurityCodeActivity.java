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

/**
 * Mock verification-code screen used between email recovery and new password entry.
 */
public class SecurityCodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_security_code);

        // This screen keeps the password-recovery flow moving through a fake code step.
        // The activity itself does not verify anything against a backend.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // The page only needs one back action and two buttons.
        ImageView ivBack = findViewById(R.id.ivBack);
        Button btnConfirm = findViewById(R.id.btnConfirm);
        Button btnSendAgain = findViewById(R.id.btnSendAgain);

        // Each box is bound separately because the layout shows six independent digits.
        // The mock code is split into six visible boxes.
        EditText etCode1 = findViewById(R.id.etCode1);
        EditText etCode2 = findViewById(R.id.etCode2);
        EditText etCode3 = findViewById(R.id.etCode3);
        EditText etCode4 = findViewById(R.id.etCode4);
        EditText etCode5 = findViewById(R.id.etCode5);
        EditText etCode6 = findViewById(R.id.etCode6);

        // Pre-fill the demo verification code so the mock flow can continue without typing.
        etCode1.setText("2");
        etCode2.setText("7");
        etCode3.setText("3");
        etCode4.setText("9");
        etCode5.setText("1");
        etCode6.setText("6");

        // The prefilled code keeps all six boxes visually consistent.
        // Back returns to the previous recovery step.
        ivBack.setOnClickListener(v -> finish());

        // Confirm continues to the password-entry screen.
        btnConfirm.setOnClickListener(v ->
                startActivity(new Intent(this, NewPasswordActivity.class)));

        // Real resend logic is outside the scope of this mocked screen.
        // Resend is intentionally a no-op in the mocked recovery flow.
        btnSendAgain.setOnClickListener(v -> { /* No-op for mock */ });
    }
}
