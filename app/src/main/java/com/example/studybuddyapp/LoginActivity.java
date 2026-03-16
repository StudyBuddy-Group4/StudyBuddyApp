package com.example.studybuddyapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
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

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogIn = findViewById(R.id.btnLogIn);
        TextView tvSignUp = findViewById(R.id.tvSignUp);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogIn.setOnClickListener(v -> {
            String usernameOrEmail = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (usernameOrEmail.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            btnLogIn.setEnabled(false);
            AuthApi api = ApiClient.getAuthApi(this);
            api.login(new LoginRequest(usernameOrEmail, password)).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    btnLogIn.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        LoginResponse body = response.body();
                        SessionManager session = new SessionManager(LoginActivity.this);
                        session.saveLoginSession(
                                body.getToken(),
                                body.getUserId(),
                                body.getUsername(),
                                body.isAdmin()
                        );

                        Log.d("Login", "onResponse: " + body.isAdmin());
                        Intent intent = new Intent(LoginActivity.this, MainHubActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra(MainHubActivity.EXTRA_IS_ADMIN, body.isAdmin());
                        startActivity(intent);
                    } else {
                        String msg = ErrorUtils.parseError(response,
                                "Login failed. Check your credentials.");
                        Toast.makeText(LoginActivity.this, msg,
                                Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    btnLogIn.setEnabled(true);
                    Toast.makeText(LoginActivity.this,
                            "Network error. Is the backend running?", Toast.LENGTH_SHORT).show();
                }
            });
        });

        tvSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, CreateAccountActivity.class)));

        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }
}
