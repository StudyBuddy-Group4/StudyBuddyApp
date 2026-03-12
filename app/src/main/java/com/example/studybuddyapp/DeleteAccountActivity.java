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

public class DeleteAccountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_delete_account);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText etPassword = findViewById(R.id.etPassword);
        ImageView ivToggle = findViewById(R.id.ivTogglePassword);

        ivToggle.setOnClickListener(v -> {
            if (etPassword.getTransformationMethod() instanceof PasswordTransformationMethod) {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            etPassword.setSelection(etPassword.length());
        });

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> {
            String password = etPassword.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
                return;
            }

            UserApi api = ApiClient.getUserApi(this);
            api.deleteAccount(new DeleteAccountRequest(password)).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.isSuccessful()) {
                        SessionManager session = new SessionManager(DeleteAccountActivity.this);
                        session.clearSession();
                        ApiClient.resetInstance();
                        Toast.makeText(DeleteAccountActivity.this,
                                "Account Deleted", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(
                                DeleteAccountActivity.this, LaunchOptionsActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        String msg = ErrorUtils.parseError(response, "Incorrect password");
                        Toast.makeText(DeleteAccountActivity.this, msg,
                                Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Toast.makeText(DeleteAccountActivity.this,
                            "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });

        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
    }
}
