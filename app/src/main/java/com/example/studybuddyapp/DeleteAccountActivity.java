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
import com.example.studybuddyapp.api.dto.DeleteAccountRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Handles account deletion confirmation, validation, and logout cleanup.
 */
public class DeleteAccountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_delete_account);

        // Apply system bar insets so the warning content stays readable on all devices.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Bind the password confirmation field and its visibility toggle.
        EditText etPassword = findViewById(R.id.etPassword);
        ImageView ivToggle = findViewById(R.id.ivTogglePassword);

        ivToggle.setOnClickListener(v -> {
            // Allow the user to verify the typed password before confirming deletion.
            if (etPassword.getTransformationMethod() instanceof PasswordTransformationMethod) {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            // Keep the cursor at the end after changing the transformation method.
            etPassword.setSelection(etPassword.length());
        });

        // The back arrow closes this screen and returns to the previous settings step.
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> {
            // Read the latest password value when the user confirms deletion.
            String password = etPassword.getText().toString().trim();

            // Require the password before making a destructive backend request.
            // This keeps accidental empty submissions away from the server.
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Only the password is needed for the backend delete-account endpoint.
            // The server still performs the final credential check before deleting the account.
            UserApi api = ApiClient.getUserApi(this);
            api.deleteAccount(new DeleteAccountRequest(password)).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    // A successful backend response means the account no longer exists.
                    if (response.isSuccessful()) {
                        // A successful deletion also removes the local session and API client state.
                        SessionManager session = new SessionManager(DeleteAccountActivity.this);
                        session.clearSession();
                        ApiClient.resetInstance();
                        Toast.makeText(DeleteAccountActivity.this,
                                "Account Deleted", Toast.LENGTH_SHORT).show();

                        // Clear the task stack so the user cannot navigate back into the account.
                        // The launch screen becomes the new entry point after deletion.
                        Intent intent = new Intent(
                                DeleteAccountActivity.this, LaunchOptionsActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        // Show the clearest backend error message available for the failed deletion.
                        // The common failure case is a wrong password, but we still parse the response first.
                        String msg = ErrorUtils.parseError(response, "Incorrect password");
                        Toast.makeText(DeleteAccountActivity.this, msg,
                                Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    // Network failures keep the user on this screen so they can retry later.
                    // Nothing local is cleared until the backend confirms deletion.
                    Toast.makeText(DeleteAccountActivity.this,
                            "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // The cancel button abandons the deletion flow without changing any data.
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
    }
}
