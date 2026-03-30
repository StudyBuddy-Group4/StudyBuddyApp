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
public class LaunchOptionsActivityInstrumentedTest {

    private LaunchOptionsActivity activity;

    @After
    public void tearDown() {
        if (activity != null) {
            activity.finish();
        }
    }

    // test that the entry screen shows the app name and tagline text
    @Test
    public void onCreate_displaysBrandingText() {
        launchActivity();

        TextView appName = activity.findViewById(R.id.tvAppName);
        TextView tagline = activity.findViewById(R.id.tvTagline);

        assertEquals(activity.getString(R.string.splash_app_name), appName.getText().toString());
        assertEquals(activity.getString(R.string.launch_tagline), tagline.getText().toString());
    }

    private void launchActivity() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, LaunchOptionsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        activity = (LaunchOptionsActivity) InstrumentationRegistry.getInstrumentation()
                .startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
