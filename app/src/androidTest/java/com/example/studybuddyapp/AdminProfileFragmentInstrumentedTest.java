package com.example.studybuddyapp;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.studybuddyapp.api.ApiClient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(AndroidJUnit4.class)
public class AdminProfileFragmentInstrumentedTest {

    private LaunchOptionsActivity activity;
    private AdminProfileFragment fragment;
    private int containerId;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        new SessionManager(context).clearSession();
        ApiClient.resetInstance();

        Intent intent = new Intent(context, LaunchOptionsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        activity = (LaunchOptionsActivity) InstrumentationRegistry.getInstrumentation()
                .startActivitySync(intent);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            FrameLayout container = new FrameLayout(activity);
            containerId = View.generateViewId();
            container.setId(containerId);
            activity.setContentView(container);

            fragment = new AdminProfileFragment();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(containerId, fragment)
                    .commitNow();
        });
    }

    @After
    public void tearDown() {
        Context context = ApplicationProvider.getApplicationContext();
        new SessionManager(context).clearSession();
        ApiClient.resetInstance();

        if (activity != null) {
            activity.finish();
        }
    }

    // test that cached admin session data is shown in the profile header
    @Test
    public void showCachedData_displaysStoredAdminNameAndId() {
        SessionManager sessionManager = new SessionManager(activity);
        sessionManager.saveLoginSession("admin-token", 99L, "adminUser", true);

        replaceFragment();

        TextView nameView = fragment.requireView().findViewById(R.id.tvAdminProfileName);
        TextView idView = fragment.requireView().findViewById(R.id.tvAdminProfileId);

        assertEquals("adminUser", nameView.getText().toString());
        assertEquals("ID: 99", idView.getText().toString());
    }

    // test that the fragment keeps its default header when no cached session exists
    @Test
    public void showCachedData_keepsDefaultHeaderWithoutSessionValues() {
        TextView nameView = fragment.requireView().findViewById(R.id.tvAdminProfileName);
        TextView idView = fragment.requireView().findViewById(R.id.tvAdminProfileId);

        assertEquals(activity.getString(R.string.mock_user_name), nameView.getText().toString());
        assertEquals(activity.getString(R.string.mock_user_id), idView.getText().toString());
    }

    // test that logout clears the stored admin session data
    @Test
    public void logout_clearsStoredSession() {
        SessionManager sessionManager = new SessionManager(activity);
        sessionManager.saveLoginSession("admin-token", 77L, "boss", true);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                fragment.requireView().findViewById(R.id.menu_logout).performClick());

        assertFalse(sessionManager.isLoggedIn());
        assertEquals("", sessionManager.getUsername());
        assertEquals(-1L, sessionManager.getUserId());
        assertFalse(sessionManager.isAdmin());
    }

    private void replaceFragment() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            fragment = new AdminProfileFragment();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(containerId, fragment)
                    .commitNow();
        });
    }
}
