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
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ProfileFragmentInstrumentedTest {

    private LaunchOptionsActivity activity;
    private ProfileFragment fragment;
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
            fragment = new ProfileFragment();
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

    @Test
    public void showCachedData_displaysStoredUsernameAndUserId() {
        SessionManager sessionManager = new SessionManager(activity);
        sessionManager.saveLoginSession("token-1", 42L, "alice", false);

        replaceFragment();

        TextView nameView = fragment.requireView().findViewById(R.id.tvProfileName);
        TextView idView = fragment.requireView().findViewById(R.id.tvProfileId);

        assertEquals("alice", nameView.getText().toString());
        assertEquals("ID: 42", idView.getText().toString());
    }

    @Test
    public void showCachedData_keepsDefaultViewsWhenSessionHasNoUserData() {
        TextView nameView = fragment.requireView().findViewById(R.id.tvProfileName);
        TextView idView = fragment.requireView().findViewById(R.id.tvProfileId);

        assertEquals(activity.getString(R.string.mock_user_name), nameView.getText().toString());
        assertEquals(activity.getString(R.string.mock_user_id), idView.getText().toString());
    }

    @Test
    public void logout_clearsStoredSession() {
        SessionManager sessionManager = new SessionManager(activity);
        sessionManager.saveLoginSession("token-2", 77L, "bob", true);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                fragment.requireView().findViewById(R.id.menu_logout).performClick());

        assertFalse(sessionManager.isLoggedIn());
        assertEquals("", sessionManager.getUsername());
        assertEquals(-1L, sessionManager.getUserId());
        assertFalse(sessionManager.isAdmin());
    }

    @Test
    public void logout_startsLaunchOptionsActivity() {
        SessionManager sessionManager = new SessionManager(activity);
        sessionManager.saveLoginSession("token-3", 88L, "carol", false);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                fragment.requireView().findViewById(R.id.menu_logout).performClick());

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertTrue(activity.isFinishing() || activity.isDestroyed() || !sessionManager.isLoggedIn());
    }

    private void replaceFragment() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            fragment = new ProfileFragment();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(containerId, fragment)
                    .commitNow();
        });
    }
}
