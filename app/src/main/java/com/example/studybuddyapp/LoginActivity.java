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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.AuthApi;
import com.example.studybuddyapp.api.ErrorUtils;
import com.example.studybuddyapp.api.dto.LoginRequest;
import com.example.studybuddyapp.api.dto.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Handles user login input, validation, and navigation into the main app flow.
 */
public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Login is the main path back into the app after sign-up, splash, and password reset.
        // Apply system bar insets so the header and form are not clipped by the device edges.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Bind the fields and actions used by the login screen.
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogIn = findViewById(R.id.btnLogIn);
        TextView tvSignUp = findViewById(R.id.tvSignUp);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // The password field uses the same visibility toggle pattern as the sign-up screen.
        setupPasswordToggle(findViewById(R.id.ivTogglePassword), etPassword);

        btnLogIn.setOnClickListener(v -> {
            // Read the latest credentials when the user tries to log in.
            String usernameOrEmail = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // Basic validation gives immediate feedback before contacting the backend.
            if (usernameOrEmail.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Lock the button while the login request is in progress.
            btnLogIn.setEnabled(false);
            // The auth API is resolved fresh here so it uses the current app context.
            AuthApi api = ApiClient.getAuthApi(this);
            api.login(new LoginRequest(usernameOrEmail, password)).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    // Re-enable the button no matter how the backend responds.
                    btnLogIn.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        // Persist the authenticated user so the rest of the app can reuse the session.
                        LoginResponse body = response.body();
                        SessionManager session = new SessionManager(LoginActivity.this);
                        session.saveLoginSession(
                                body.getToken(),
                                body.getUserId(),
                                body.getUsername(),
                                body.isAdmin()
                        );

                        // Start the main hub and clear the login screen from the back stack.
                        Intent intent = new Intent(LoginActivity.this, MainHubActivity.class);
                        // Clearing the stack stops the user from returning to the login form with the back button.
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra(MainHubActivity.EXTRA_IS_ADMIN, body.isAdmin());
                        startActivity(intent);
                    } else {
                        // Show the most useful backend error message we can parse.
                        String msg = ErrorUtils.parseError(response,
                                "Login failed. Check your credentials.");
                        Toast.makeText(LoginActivity.this, msg,
                                Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    // Network failures also need to unlock the button so the user can retry.
                    btnLogIn.setEnabled(true);
                    // The current email and password stay in the fields so the retry is easy.
                    Toast.makeText(LoginActivity.this,
                            "Network error. Is the backend running?", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Secondary links keep the other auth flows reachable from the login page.
        tvSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, CreateAccountActivity.class)));

        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    /**
     * Toggles the password field between hidden and visible text.
     */
    private void setupPasswordToggle(ImageView toggle, EditText editText) {
        toggle.setOnClickListener(v -> {
            // Swap the transformation method so the user can verify the typed password.
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
