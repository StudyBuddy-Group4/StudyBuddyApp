package com.example.studybuddyapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.menu_edit_profile).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        view.findViewById(R.id.menu_security).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SecurityMenuActivity.class)));

        view.findViewById(R.id.menu_setting).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsMenuActivity.class)));

        view.findViewById(R.id.menu_logout).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LaunchOptionsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
