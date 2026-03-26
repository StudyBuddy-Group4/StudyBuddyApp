package com.example.studybuddyapp;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SessionManagerInstrumentedTest {

    private static final String PREF_NAME = "studybuddy_session";

    private Context context;
    private SessionManager sessionManager;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        clearStoredSession();
        sessionManager = new SessionManager(context);
    }

    @Test
    public void saveLoginSession_storesAllSessionFields() {
        sessionManager.saveLoginSession("token-123", 42L, "a", true);

        assertEquals("token-123", sessionManager.getToken());
        assertEquals(42L, sessionManager.getUserId());
        assertEquals("a", sessionManager.getUsername());
        assertTrue(sessionManager.isAdmin());
        assertTrue(sessionManager.isLoggedIn());
    }

    @Test
    public void isLoggedIn_returnsFalseWhenNoTokenStored() {
        assertEquals("", sessionManager.getToken());
        assertEquals(-1L, sessionManager.getUserId());
        assertEquals("", sessionManager.getUsername());
        assertFalse(sessionManager.isAdmin());
        assertFalse(sessionManager.isLoggedIn());
    }

    @Test
    public void clearSession_removesStoredValues() {
        sessionManager.saveLoginSession("token-456", 7L, "b", false);

        sessionManager.clearSession();

        assertEquals("", sessionManager.getToken());
        assertEquals(-1L, sessionManager.getUserId());
        assertEquals("", sessionManager.getUsername());
        assertFalse(sessionManager.isAdmin());
        assertFalse(sessionManager.isLoggedIn());
    }

    private void clearStoredSession() {
        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
    }
}
