package com.example.studybuddyapp;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class PasswordChangedActivityInstrumentedTest {

    private PasswordChangedActivity activity;

    @After
    public void tearDown() {
        if (activity != null) {
            activity.finish();
        }
    }

    // test that the success screen shows the password-changed message
    @Test
    public void onCreate_displaysPasswordChangedMessage() {
        launchActivity();

        TextView messageView = activity.findViewById(R.id.tvMessage);

        assertEquals(activity.getString(R.string.password_changed_success),
                messageView.getText().toString());
    }

    // test that the success screen returns the user to login after the delay
    @Test
    public void delayedTransition_startsLoginActivity() {
        launchActivity();

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                LoginActivity.class.getName(), null, false);

        LoginActivity nextActivity =
                (LoginActivity) instrumentation.waitForMonitorWithTimeout(monitor, 3500);

        assertNotNull(nextActivity);
        nextActivity.finish();
    }

    private void launchActivity() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, PasswordChangedActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        activity = (PasswordChangedActivity) InstrumentationRegistry.getInstrumentation()
                .startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
