package com.example.studybuddyapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainHubActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_hub);

        bottomNavigation = findViewById(R.id.bottom_navigation);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            bottomNavigation.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_tasks) {
                selectedFragment = new TaskListFragment();
            } else if (itemId == R.id.nav_statistics) {
                selectedFragment = new StatisticsFragment();
            } else if (itemId == R.id.nav_profile) {
                
                // ADMIN CHECK LOGIC
                // For testing purposes, we hardcode this to be true/false.
                // Later, we will replace this with an actual check from login system/database.
                boolean isAdmin = false;
                
                if (isAdmin) {
                    selectedFragment = new AdminProfileFragment(); // Loads the Admin Layout
                } else {
                    selectedFragment = new ProfileFragment();      // Loads the Standard Layout
                }
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        if (savedInstanceState == null) {
            if (getIntent().getBooleanExtra("navigate_to_tasks", false)) {
                bottomNavigation.setSelectedItemId(R.id.nav_tasks);
            } else {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            }
        }
    }

    public void switchToTasksTab() {
        bottomNavigation.setSelectedItemId(R.id.nav_tasks);
    }
}
