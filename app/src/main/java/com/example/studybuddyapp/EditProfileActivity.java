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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        tvHeaderName = findViewById(R.id.tvHeaderName);
        tvHeaderId = findViewById(R.id.tvHeaderId);

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        loadProfile();

        findViewById(R.id.btnUpdateProfile).setOnClickListener(v -> updateProfile());
    }

    private void loadProfile() {
        UserApi api = ApiClient.getUserApi(this);
        api.getProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call,
                                   Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserProfileResponse profile = response.body();
                    etUsername.setText(profile.getUsername());
                    etEmail.setText(profile.getEmail());
                    tvHeaderName.setText(profile.getUsername());
                    tvHeaderId.setText("ID: " + profile.getId());
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                SessionManager session = new SessionManager(EditProfileActivity.this);
                etUsername.setText(session.getUsername());
                tvHeaderName.setText(session.getUsername());
                long uid = session.getUserId();
                if (uid > 0) tvHeaderId.setText("ID: " + uid);
            }
        });
    }

    private void updateProfile() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        UserApi api = ApiClient.getUserApi(this);
        api.updateProfile(new UpdateProfileRequest(username, email)).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    SessionManager session = new SessionManager(EditProfileActivity.this);
                    session.saveLoginSession(session.getToken(), session.getUserId(),
                            username, session.isAdmin());
                    tvHeaderName.setText(username);
                    Toast.makeText(EditProfileActivity.this,
                            "Profile Updated", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String msg = ErrorUtils.parseError(response, "Update failed");
                    Toast.makeText(EditProfileActivity.this, msg,
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(EditProfileActivity.this,
                        "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
