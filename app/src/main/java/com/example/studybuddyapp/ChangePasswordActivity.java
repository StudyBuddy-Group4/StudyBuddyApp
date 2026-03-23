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

public class ChangePasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText etCurrent = findViewById(R.id.etCurrentPassword);
        EditText etNew = findViewById(R.id.etNewPassword);
        EditText etConfirm = findViewById(R.id.etConfirmPassword);

        setupPasswordToggle(findViewById(R.id.ivToggleCurrent), etCurrent);
        setupPasswordToggle(findViewById(R.id.ivToggleNew), etNew);
        setupPasswordToggle(findViewById(R.id.ivToggleConfirm), etConfirm);

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnChangePassword).setOnClickListener(v -> {
            String current = etCurrent.getText().toString().trim();
            String newPw = etNew.getText().toString().trim();
            String confirm = etConfirm.getText().toString().trim();

            if (current.isEmpty() || newPw.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPw.equals(confirm)) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPw.equals(current)) {
                Toast.makeText(this, "New password must be different from current",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, String> body = new HashMap<>();
            body.put("currentPassword", current);
            body.put("newPassword", newPw);

            UserApi api = ApiClient.getUserApi(this);
            api.changePassword(body).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.isSuccessful()) {
                        startActivity(new Intent(ChangePasswordActivity.this,
                                PasswordChangedSuccessActivity.class));
                        finish();
                    } else {
                        String msg = ErrorUtils.parseError(response,
                                "Failed to change password.");
                        Toast.makeText(ChangePasswordActivity.this, msg,
                                Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Toast.makeText(ChangePasswordActivity.this,
                            "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

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
