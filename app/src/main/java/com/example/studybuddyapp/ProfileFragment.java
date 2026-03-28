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

/**
 * Shows the user's profile details and links to profile-related actions.
 */
public class ProfileFragment extends Fragment {

    // These labels show the current username and numeric id at the top of the profile tab.
    private TextView tvName;
    private TextView tvId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // The profile tab is simple enough to be recreated directly from its XML layout.
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind the header labels that are updated from cached or backend profile data.
        tvName = view.findViewById(R.id.tvProfileName);
        tvId = view.findViewById(R.id.tvProfileId);

        // Show whatever data is already available locally before the backend refresh finishes.
        showCachedData();

        view.findViewById(R.id.menu_edit_profile).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        view.findViewById(R.id.menu_security).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SecurityMenuActivity.class)));

        view.findViewById(R.id.menu_setting).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsMenuActivity.class)));

        view.findViewById(R.id.menu_logout).setOnClickListener(v -> {
            // Logging out clears both local session data and any cached API client state.
            new SessionManager(requireContext()).clearSession();
            ApiClient.resetInstance();
            Intent intent = new Intent(requireContext(), LaunchOptionsActivity.class);
            // Clearing the task stack prevents navigating back into authenticated screens.
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // The launch screen becomes the new root once the old session is gone.
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh on resume so changes from edit/security screens are reflected immediately.
        loadProfileFromBackend();
    }

    /**
     * Displays any profile data that is already stored locally in the session.
     */
    private void showCachedData() {
        SessionManager session = new SessionManager(requireContext());
        // Username is optional in storage, so only overwrite the label when a value exists.
        String username = session.getUsername();
        if (username != null && !username.isEmpty()) {
            tvName.setText(username);
        }
        long userId = session.getUserId();
        if (userId > 0) {
            // Only show an id label when the session actually contains a valid stored id.
            tvId.setText("ID: " + userId);
        }
    }

    /**
     * Refreshes the profile from the backend and updates the cached session values.
     */
    private void loadProfileFromBackend() {
        UserApi api = ApiClient.getUserApi(requireContext());
        api.getProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call,
                                   Response<UserProfileResponse> response) {
                // Ignore callbacks after the fragment has been detached from its activity.
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    UserProfileResponse profile = response.body();
                    // Replace the cached placeholder values with fresh backend data.
                    tvName.setText(profile.getUsername());
                    tvId.setText("ID: " + profile.getId());

                    // Keep the local session in sync with the latest backend profile data.
                    SessionManager session = new SessionManager(requireContext());
                    session.saveLoginSession(session.getToken(), profile.getId(),
                            profile.getUsername(), profile.isAdmin());
                }
                // Unsuccessful responses leave the last visible cached values untouched.
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                // Keep the cached values already shown on screen.
            }
        });
    }
}
