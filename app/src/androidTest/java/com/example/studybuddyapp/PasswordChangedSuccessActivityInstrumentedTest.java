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
public class PasswordChangedSuccessActivityInstrumentedTest {

    private PasswordChangedSuccessActivity activity;

    @After
    public void tearDown() {
        if (activity != null) {
            activity.finish();
        }
    }

    // test that the screen shows the password-changed success message
    @Test
    public void onCreate_displaysSuccessMessage() {
        launchActivity();

        TextView messageView = activity.findViewById(R.id.tvMessage);

        assertEquals(activity.getString(R.string.password_changed_success),
                messageView.getText().toString());
    }

    // test that the delayed transition opens the main hub screen
    @Test
    public void delayedTransition_startsMainHubActivity() {
        launchActivity();

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                MainHubActivity.class.getName(), null, false);

        MainHubActivity nextActivity =
                (MainHubActivity) instrumentation.waitForMonitorWithTimeout(monitor, 3500);

        assertNotNull(nextActivity);
        nextActivity.finish();
    }

    private void launchActivity() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, PasswordChangedSuccessActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        activity = (PasswordChangedSuccessActivity) InstrumentationRegistry.getInstrumentation()
                .startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
