package com.example.studybuddyapp;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.ErrorUtils;
import com.example.studybuddyapp.api.UserApi;
import com.example.studybuddyapp.api.dto.UpdateProfileRequest;
import com.example.studybuddyapp.api.dto.UserProfileResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Lets the user review and update their profile details.
 */
public class EditProfileActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etEmail;
    private TextView tvHeaderName;
    private TextView tvHeaderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        // Apply system bar insets so the profile form stays readable on different devices.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Bind the editable profile fields and header summary views.
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        tvHeaderName = findViewById(R.id.tvHeaderName);
        tvHeaderId = findViewById(R.id.tvHeaderId);

        // The back arrow closes this screen and returns to the previous profile screen.
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        // Load the latest profile values as soon as the screen opens.
        loadProfile();

        // The update button submits the current field values.
        findViewById(R.id.btnUpdateProfile).setOnClickListener(v -> updateProfile());
    }

    /**
     * Loads the latest profile values from the backend and falls back to cached session data if needed.
     */
    private void loadProfile() {
        // Prefer fresh backend data so the form reflects the server's current profile values.
        UserApi api = ApiClient.getUserApi(this);
        api.getProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call,
                                   Response<UserProfileResponse> response) {
                // Only update the UI when the backend returns a complete profile object.
                if (response.isSuccessful() && response.body() != null) {
                    // A successful profile response refreshes both the editable fields and header.
                    UserProfileResponse profile = response.body();
                    etUsername.setText(profile.getUsername());
                    etEmail.setText(profile.getEmail());
                    tvHeaderName.setText(profile.getUsername());
                    tvHeaderId.setText("ID: " + profile.getId());
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                // If the backend cannot be reached, keep the screen usable with cached session data.
                SessionManager session = new SessionManager(EditProfileActivity.this);

                // The cached username is still useful for both the header and the editable field.
                etUsername.setText(session.getUsername());
                tvHeaderName.setText(session.getUsername());
                long uid = session.getUserId();

                // Only show the numeric id header when the session actually has one stored.
                if (uid > 0) tvHeaderId.setText("ID: " + uid);
            }
        });
    }

    /**
     * Validates the form and sends the updated profile values to the backend.
     */
    private void updateProfile() {
        // Read the latest values entered by the user.
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        // Basic validation avoids sending incomplete profile updates.
        if (username.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Send the latest username and email as a profile update request.
        // The backend remains the source of truth, but the screen updates immediately after success.
        UserApi api = ApiClient.getUserApi(this);
        api.updateProfile(new UpdateProfileRequest(username, email)).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                // A successful update should be reflected immediately in both the session and header.
                if (response.isSuccessful()) {
                    // Keep the locally cached session aligned with the updated username.
                    SessionManager session = new SessionManager(EditProfileActivity.this);
                    session.saveLoginSession(session.getToken(), session.getUserId(),
                            username, session.isAdmin());

                    // Refresh the header immediately so the user sees the new name right away.
                    tvHeaderName.setText(username);
                    Toast.makeText(EditProfileActivity.this,
                            "Profile Updated", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    // Show the clearest backend error we can extract from the response.
                    // This keeps validation errors visible without duplicating parsing logic here.
                    String msg = ErrorUtils.parseError(response, "Update failed");
                    Toast.makeText(EditProfileActivity.this, msg,
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                // Network failures keep the user on this screen so they can retry later.
                // The current field values stay in place so the user does not lose their edits.
                Toast.makeText(EditProfileActivity.this,
                        "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
