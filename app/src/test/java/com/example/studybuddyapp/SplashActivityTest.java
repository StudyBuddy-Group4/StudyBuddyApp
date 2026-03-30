package com.example.studybuddyapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Looper;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.time.Duration;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SplashActivityTest {

    @Test
    public void onCreate_displaysSplashAppName() {
        SplashActivity activity = Robolectric.buildActivity(SplashActivity.class).setup().get();

        TextView appNameView = activity.findViewById(R.id.tvAppName);

        assertEquals(activity.getString(R.string.splash_app_name), appNameView.getText().toString());
    }

    @Test
    public void splashDelay_startsLaunchOptionsActivity() {
        SplashActivity activity = Robolectric.buildActivity(SplashActivity.class).setup().get();

        ShadowLooper shadowLooper = shadowOf(Looper.getMainLooper());
        shadowLooper.idleFor(Duration.ofSeconds(2));

        Intent nextIntent = shadowOf(activity).getNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(LaunchOptionsActivity.class.getName(), nextIntent.getComponent().getClassName());
    }
}
