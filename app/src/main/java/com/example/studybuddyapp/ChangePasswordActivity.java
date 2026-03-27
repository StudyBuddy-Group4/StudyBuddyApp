package com.example.studybuddyapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.ErrorUtils;
import com.example.studybuddyapp.api.UserApi;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Handles password change input, validation, and the success redirect flow.
 */
public class ChangePasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);

        // Apply system bar insets so the form remains readable on different screen shapes.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Bind the three password fields used by the change-password form.
        EditText etCurrent = findViewById(R.id.etCurrentPassword);
        EditText etNew = findViewById(R.id.etNewPassword);
        EditText etConfirm = findViewById(R.id.etConfirmPassword);

        // Each password field can be toggled between hidden and visible text.
        setupPasswordToggle(findViewById(R.id.ivToggleCurrent), etCurrent);
        setupPasswordToggle(findViewById(R.id.ivToggleNew), etNew);
        setupPasswordToggle(findViewById(R.id.ivToggleConfirm), etConfirm);

        // The back arrow closes this screen and returns to the previous settings step.
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnChangePassword).setOnClickListener(v -> {
            // Read the latest values when the user submits the form.
            String current = etCurrent.getText().toString().trim();
            String newPw = etNew.getText().toString().trim();
            String confirm = etConfirm.getText().toString().trim();

            // Basic validation avoids unnecessary backend requests and gives faster feedback.
            if (current.isEmpty() || newPw.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // The confirmation password must match before we attempt an update.
            if (!newPw.equals(confirm)) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Reject reusing the current password before sending the request.
            if (newPw.equals(current)) {
                Toast.makeText(this, "New password must be different from current",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Build the request body expected by the backend password-change endpoint.
            Map<String, String> body = new HashMap<>();
            body.put("currentPassword", current);
            body.put("newPassword", newPw);

            UserApi api = ApiClient.getUserApi(this);
            api.changePassword(body).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.isSuccessful()) {
                        // A successful change sends the user to the confirmation screen.
                        startActivity(new Intent(ChangePasswordActivity.this,
                                PasswordChangedSuccessActivity.class));
                        finish();
                    } else {
                        // Surface the clearest backend error message we can extract.
                        String msg = ErrorUtils.parseError(response,
                                "Failed to change password.");
                        Toast.makeText(ChangePasswordActivity.this, msg,
                                Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    // Network failures keep the user on this form so they can try again later.
                    Toast.makeText(ChangePasswordActivity.this,
                            "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * Toggles a password field between hidden and visible text.
     */
    private void setupPasswordToggle(ImageView toggle, EditText editText) {
        toggle.setOnClickListener(v -> {
            // Swap the transformation method so the user can verify typed passwords.
            if (editText.getTransformationMethod() instanceof PasswordTransformationMethod) {
                editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                // Match the icon to the visible-text state.
                toggle.setImageResource(R.drawable.ic_visibility);
            } else {
                editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                // Switch back to the hidden-password icon when masking the field again.
                toggle.setImageResource(R.drawable.ic_visibility_off);
            }
            // Keep the cursor at the end after changing the transformation method.
            editText.setSelection(editText.length());
        });
    }
}
