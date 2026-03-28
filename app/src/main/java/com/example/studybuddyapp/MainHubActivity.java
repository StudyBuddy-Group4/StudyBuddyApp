package com.example.studybuddyapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Main container screen for the app's bottom-navigation flow.
 */
public class MainHubActivity extends AppCompatActivity {

    // Optional admin flag passed from login
    public static final String EXTRA_IS_ADMIN = "extra_is_admin";

    // Bottom navigation bar
    private BottomNavigationView bottomNavigation;
    // Current role
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_hub);

        // Read the role from session or login extras
        SessionManager session = new SessionManager(this);
        isAdmin = session.isAdmin() || getIntent().getBooleanExtra(EXTRA_IS_ADMIN, false);

        // Navigation bar
        bottomNavigation = findViewById(R.id.bottom_navigation);

        // Keep the bottom bar above the system inset
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            bottomNavigation.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        if (isAdmin) {
            // Admin uses the reduced navigation menu
            setupAdminNavigation();
        } else {
            // Regular users get the full tab set
            setupRegularNavigation(savedInstanceState);
        }
    }

    /**
     * Sets up the admin-only bottom navigation.
     */
    private void setupAdminNavigation() {
        // Replace the regular menu with the admin menu
        bottomNavigation.getMenu().clear();
        bottomNavigation.inflateMenu(R.menu.bottom_nav_menu_admin);

        // Admin starts on the profile/report screen
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new AdminProfileFragment())
                .commit();

        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_profile) {
                // Admin navigation currently has one root fragment
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new AdminProfileFragment())
                        .commit();
            }
            return true;
        });

        // Keep the menu state in sync with the shown fragment
        bottomNavigation.setSelectedItemId(R.id.nav_profile);
    }

    /**
     * Sets up the regular user bottom navigation.
     */
    private void setupRegularNavigation(Bundle savedInstanceState) {
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // Home tab
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_tasks) {
                // Task tab
                selectedFragment = new TaskListFragment();
            } else if (itemId == R.id.nav_statistics) {
                // Statistics tab
                selectedFragment = new StatisticsFragment();
            } else if (itemId == R.id.nav_profile) {
                // Profile tab
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                // Swap the fragment inside the shared container
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        if (savedInstanceState == null) {
            if (getIntent().getBooleanExtra("navigate_to_tasks", false)) {
                // Some flows open the hub directly on the task tab
                bottomNavigation.setSelectedItemId(R.id.nav_tasks);
            } else {
                // Home is the default landing tab
                bottomNavigation.setSelectedItemId(R.id.nav_home);
            }
        }
    }

    /**
     * Switches the regular-user navigation to the tasks tab.
     */
    public void switchToTasksTab() {
        if (!isAdmin) {
            // Admin navigation does not expose the task tab
            bottomNavigation.setSelectedItemId(R.id.nav_tasks);
        }
    }
}
