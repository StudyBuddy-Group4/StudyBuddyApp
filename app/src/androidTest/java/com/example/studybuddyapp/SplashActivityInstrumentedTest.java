package com.example.studybuddyapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

@RunWith(AndroidJUnit4.class)
public class SplashActivityInstrumentedTest {

    private SplashActivity activity;

    @After
    public void tearDown() {
        if (activity != null) {
            activity.finish();
        }
    }

    @Test
    public void onCreate_displaysSplashAppName() {
        launchActivity();

        TextView appNameView = activity.findViewById(R.id.tvAppName);

        assertEquals(activity.getString(R.string.splash_app_name), appNameView.getText().toString());
    }

    @Test
    public void splashDelay_startsLaunchOptionsActivity() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                LaunchOptionsActivity.class.getName(), null, false);

        launchActivity();

        LaunchOptionsActivity nextActivity =
                (LaunchOptionsActivity) instrumentation.waitForMonitorWithTimeout(monitor, 3500);

        assertNotNull(nextActivity);
        nextActivity.finish();
        instrumentation.removeMonitor(monitor);
    }

    private void launchActivity() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        activity = (SplashActivity) InstrumentationRegistry.getInstrumentation()
                .startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
