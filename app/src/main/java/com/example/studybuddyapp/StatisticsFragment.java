package com.example.studybuddyapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

public class StatisticsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.ic_question).setOnClickListener(v -> showRatingExplanation());
    }

    private void showRatingExplanation() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.how_rating_calculated)
                .setMessage(R.string.rating_explanation)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
