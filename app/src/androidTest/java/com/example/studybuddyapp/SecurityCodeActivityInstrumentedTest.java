package com.example.studybuddyapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.widget.EditText;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SecurityCodeActivityInstrumentedTest {

    private SecurityCodeActivity activity;

    @After
    public void tearDown() {
        if (activity != null) {
            activity.finish();
        }
    }

    @Test
    public void onCreate_prefillsMockSecurityCode() {
        launchActivity();

        assertEquals("2", ((EditText) activity.findViewById(R.id.etCode1)).getText().toString());
        assertEquals("7", ((EditText) activity.findViewById(R.id.etCode2)).getText().toString());
        assertEquals("3", ((EditText) activity.findViewById(R.id.etCode3)).getText().toString());
        assertEquals("9", ((EditText) activity.findViewById(R.id.etCode4)).getText().toString());
        assertEquals("1", ((EditText) activity.findViewById(R.id.etCode5)).getText().toString());
        assertEquals("6", ((EditText) activity.findViewById(R.id.etCode6)).getText().toString());
    }

    @Test
    public void confirmButton_startsNewPasswordActivity() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                NewPasswordActivity.class.getName(), null, false);

        launchActivity();
        instrumentation.runOnMainSync(() ->
                activity.findViewById(R.id.btnConfirm).performClick());
        instrumentation.waitForIdleSync();

        NewPasswordActivity nextActivity =
                (NewPasswordActivity) instrumentation.waitForMonitorWithTimeout(monitor, 2000);

        assertNotNull(nextActivity);
        nextActivity.finish();
        instrumentation.removeMonitor(monitor);
    }

    private void launchActivity() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, SecurityCodeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        activity = (SecurityCodeActivity) InstrumentationRegistry.getInstrumentation()
                .startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
