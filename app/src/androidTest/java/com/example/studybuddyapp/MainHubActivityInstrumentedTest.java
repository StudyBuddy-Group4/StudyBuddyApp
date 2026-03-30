package com.example.studybuddyapp;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MainHubActivityInstrumentedTest {

    private Context context;
    private SessionManager sessionManager;
    private MainHubActivity activity;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        sessionManager = new SessionManager(context);
        sessionManager.clearSession();
    }

    @After
    public void tearDown() {
        sessionManager.clearSession();
        if (activity != null) {
            activity.finish();
        }
    }

    // test that a regular user starts on the home tab by default
    @Test
    public void onCreate_regularUserShowsHomeFragmentByDefault() {
        launchMainHub(false, false);

        BottomNavigationView bottomNavigation = activity.findViewById(R.id.bottom_navigation);

        assertEquals(R.id.nav_home, bottomNavigation.getSelectedItemId());
        assertTrue(activity.getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container) instanceof HomeFragment);
    }

    // test that the task shortcut opens the hub on the task tab
    @Test
    public void onCreate_navigateToTasksExtraShowsTaskListFragment() {
        launchMainHub(false, true);

        BottomNavigationView bottomNavigation = activity.findViewById(R.id.bottom_navigation);

        assertEquals(R.id.nav_tasks, bottomNavigation.getSelectedItemId());
        assertTrue(activity.getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container) instanceof TaskListFragment);
    }

    // test that an admin session switches to the admin menu and admin profile fragment
    @Test
    public void onCreate_adminSessionUsesAdminNavigation() {
        sessionManager.saveLoginSession("token-admin", 9L, "admin", true);

        launchMainHub(false, false);

        BottomNavigationView bottomNavigation = activity.findViewById(R.id.bottom_navigation);

        assertEquals(1, bottomNavigation.getMenu().size());
        assertEquals(R.id.nav_profile, bottomNavigation.getSelectedItemId());
        assertTrue(activity.getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container) instanceof AdminProfileFragment);
    }

    // test that switchToTasksTab moves a regular user from home to the task tab
    @Test
    public void switchToTasksTab_regularUserSelectsTaskTab() {
        launchMainHub(false, false);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                activity.switchToTasksTab());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        BottomNavigationView bottomNavigation = activity.findViewById(R.id.bottom_navigation);

        assertEquals(R.id.nav_tasks, bottomNavigation.getSelectedItemId());
        assertTrue(activity.getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container) instanceof TaskListFragment);
    }

    private void launchMainHub(boolean isAdminExtra, boolean navigateToTasks) {
        Intent intent = new Intent(context, MainHubActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(MainHubActivity.EXTRA_IS_ADMIN, isAdminExtra);
        intent.putExtra("navigate_to_tasks", navigateToTasks);

        activity = (MainHubActivity) InstrumentationRegistry.getInstrumentation()
                .startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
