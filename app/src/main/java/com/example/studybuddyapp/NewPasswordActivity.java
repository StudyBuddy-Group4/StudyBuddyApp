package com.example.studybuddyapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Lets the user enter and confirm a new password.
 */
public class NewPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_password);

        // This screen keeps the password-reset flow on a single simple form.
        // Apply system-bar padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Screen actions
        ImageView ivBack = findViewById(R.id.ivBack);
        Button btnChangePassword = findViewById(R.id.btnChangePassword);

        // Password fields
        EditText etNew = findViewById(R.id.etNewPassword);
        EditText etConfirm = findViewById(R.id.etConfirmNewPassword);

        // Both fields reuse the same visibility toggle behaviour.
        // Toggle password visibility
        setupPasswordToggle(findViewById(R.id.ivToggleNewPassword), etNew);
        setupPasswordToggle(findViewById(R.id.ivToggleConfirmPassword), etConfirm);

        // Back closes this screen
        ivBack.setOnClickListener(v -> finish());

        // Continue to the confirmation screen
        btnChangePassword.setOnClickListener(v ->
                startActivity(new Intent(this, PasswordChangedActivity.class)));
    }

    /**
     * Toggles one password field between hidden and visible text.
     */
    private void setupPasswordToggle(ImageView toggle, EditText editText) {
        toggle.setOnClickListener(v -> {
            // Show the password text
            if (editText.getTransformationMethod() instanceof PasswordTransformationMethod) {
                editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                toggle.setImageResource(R.drawable.ic_visibility);
            } else {
                // Hide the password text again
                editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                toggle.setImageResource(R.drawable.ic_visibility_off);
            }
            // Keep the cursor at the end
            editText.setSelection(editText.length());
        });
    }
}
