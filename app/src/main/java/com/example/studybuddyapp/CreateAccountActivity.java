package com.example.studybuddyapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.AuthApi;
import com.example.studybuddyapp.api.ErrorUtils;
import com.example.studybuddyapp.api.dto.RegisterRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Handles account creation input, validation, and navigation back to login.
 */
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
        EditText etUsername = findViewById(R.id.etUsername);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);
        Button btnSignUp = findViewById(R.id.btnSignUp);
        TextView tvLogIn = findViewById(R.id.tvLogIn);

        ivBack.setOnClickListener(v -> finish());

        setupPasswordToggle(findViewById(R.id.ivTogglePassword), etPassword);
        setupPasswordToggle(findViewById(R.id.ivToggleConfirmPassword), etConfirmPassword);

        btnSignUp.setOnClickListener(v -> {
            // Read the latest form values when the user submits the sign-up form.
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // Basic client-side validation avoids unnecessary backend requests.
            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Lock the button while the registration request is in progress.
            btnSignUp.setEnabled(false);
            AuthApi api = ApiClient.getAuthApi(this);
            api.register(new RegisterRequest(username, email, password)).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    // Re-enable the button no matter how the backend responds.
                    btnSignUp.setEnabled(true);
                    if (response.isSuccessful()) {
                        // A successful registration returns the user to the login flow.
                        new AlertDialog.Builder(CreateAccountActivity.this)
                                .setTitle(R.string.congratulations)
                                .setMessage(R.string.account_created_msg)
                                .setPositiveButton(R.string.btn_back, (dialog, which) -> {
                                    Intent intent = new Intent(
                                            CreateAccountActivity.this, LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    startActivity(intent);
                                    finish();
                                })
                                .setCancelable(false)
                                .show();
                    } else {
                        // Show the clearest backend error we can extract from the response.
                        String msg = ErrorUtils.parseError(response,
                                "Registration failed. Check your input.");
                        Toast.makeText(CreateAccountActivity.this, msg,
                                Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    // Network failures also need to unlock the button for another attempt.
                    btnSignUp.setEnabled(true);
                    Toast.makeText(CreateAccountActivity.this,
                            "Network error. Is the backend running?", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // The footer text is a shortcut back to the existing login screen.
        tvLogIn.setOnClickListener(v -> finish());
    }

    /**
     * Toggles a password field between hidden and visible text.
     */
    private void setupPasswordToggle(ImageView toggle, EditText editText) {
        toggle.setOnClickListener(v -> {
            if (editText.getTransformationMethod() instanceof PasswordTransformationMethod) {
                editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                toggle.setImageResource(R.drawable.ic_visibility);
            } else {
                editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                toggle.setImageResource(R.drawable.ic_visibility_off);
            }
            editText.setSelection(editText.length());
        });
    }
}
