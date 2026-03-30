package com.example.studybuddyapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SessionManagerTest {

    private SessionManager sessionManager;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        sessionManager = new SessionManager(context);
        sessionManager.clearSession();
    }

    @Test
    public void saveLoginSession_persistsAllFields() {
        sessionManager.saveLoginSession("jwt-token", 123L, "alice", true);

        assertEquals("jwt-token", sessionManager.getToken());
        assertEquals(123L, sessionManager.getUserId());
        assertEquals("alice", sessionManager.getUsername());
        assertTrue(sessionManager.isAdmin());
        assertTrue(sessionManager.isLoggedIn());
    }

    @Test
    public void clearSession_removesStoredValues() {
        sessionManager.saveLoginSession("jwt-token", 123L, "alice", true);

        sessionManager.clearSession();

        assertEquals("", sessionManager.getToken());
        assertEquals(-1L, sessionManager.getUserId());
        assertEquals("", sessionManager.getUsername());
        assertFalse(sessionManager.isAdmin());
        assertFalse(sessionManager.isLoggedIn());
    }
}
