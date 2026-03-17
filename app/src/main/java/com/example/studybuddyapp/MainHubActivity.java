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

    public static final String EXTRA_IS_ADMIN = "extra_is_admin";

    private BottomNavigationView bottomNavigation;
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_hub);

        SessionManager session = new SessionManager(this);
        isAdmin = session.isAdmin() || getIntent().getBooleanExtra(EXTRA_IS_ADMIN, false);

        bottomNavigation = findViewById(R.id.bottom_navigation);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            bottomNavigation.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        if (isAdmin) {
            setupAdminNavigation();
        } else {
            setupRegularNavigation(savedInstanceState);
        }
    }

    private void setupAdminNavigation() {
        bottomNavigation.getMenu().clear();
        bottomNavigation.inflateMenu(R.menu.bottom_nav_menu_admin);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new AdminProfileFragment())
                .commit();

        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_profile) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new AdminProfileFragment())
                        .commit();
            }
            return true;
        });

        bottomNavigation.setSelectedItemId(R.id.nav_profile);
    }

    private void setupRegularNavigation(Bundle savedInstanceState) {
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
                selectedFragment = new ProfileFragment();
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
                bottomNavigation.setSelectedItemId(R.id.nav_home);
            }
        }
    }

    public void switchToTasksTab() {
        if (!isAdmin) {
            bottomNavigation.setSelectedItemId(R.id.nav_tasks);
        }
    }
}
