package com.example.studybuddyapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.studybuddyapp.api.ApiClient;
import com.example.studybuddyapp.api.UserApi;
import com.example.studybuddyapp.api.dto.UserProfileResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private TextView tvName;
    private TextView tvId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvName = view.findViewById(R.id.tvProfileName);
        tvId = view.findViewById(R.id.tvProfileId);

        showCachedData();

        view.findViewById(R.id.menu_edit_profile).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        view.findViewById(R.id.menu_security).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SecurityMenuActivity.class)));

        view.findViewById(R.id.menu_setting).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsMenuActivity.class)));

        view.findViewById(R.id.menu_logout).setOnClickListener(v -> {
            new SessionManager(requireContext()).clearSession();
            ApiClient.resetInstance();
            Intent intent = new Intent(requireContext(), LaunchOptionsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileFromBackend();
    }

    private void showCachedData() {
        SessionManager session = new SessionManager(requireContext());
        String username = session.getUsername();
        if (username != null && !username.isEmpty()) {
            tvName.setText(username);
        }
        long userId = session.getUserId();
        if (userId > 0) {
            tvId.setText("ID: " + userId);
        }
    }

    private void loadProfileFromBackend() {
        UserApi api = ApiClient.getUserApi(requireContext());
        api.getProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call,
                                   Response<UserProfileResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    UserProfileResponse profile = response.body();
                    tvName.setText(profile.getUsername());
                    tvId.setText("ID: " + profile.getId());

                    SessionManager session = new SessionManager(requireContext());
                    session.saveLoginSession(session.getToken(), profile.getId(),
                            profile.getUsername(), profile.isAdmin());
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                // Keep showing cached data
            }
        });
    }
}
