package com.example.studybuddyapp;

import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class NotificationSettingsActivityInstrumentedTest {

    private NotificationSettingsActivity activity;

    @After
    public void tearDown() {
        if (activity != null) {
            activity.finish();
        }
    }

    // test that the screen shows the notification title and starts with the switch enabled
    @Test
    public void onCreate_displaysNotificationTitleAndEnabledSwitch() {
        launchActivity();

        TextView titleView = activity.findViewById(R.id.tvTitle);
        SwitchCompat notificationSwitch = activity.findViewById(R.id.switchNotification);

        assertEquals(activity.getString(R.string.notification_settings),
                titleView.getText().toString());
        assertTrue(notificationSwitch.isChecked());
    }

    // test that the notification switch can be turned off from its default state
    @Test
    public void switchNotificationClick_togglesSwitchOff() {
        launchActivity();

        SwitchCompat notificationSwitch = activity.findViewById(R.id.switchNotification);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(notificationSwitch::performClick);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertFalse(notificationSwitch.isChecked());
    }

    private void launchActivity() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, NotificationSettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        activity = (NotificationSettingsActivity) InstrumentationRegistry.getInstrumentation()
                .startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
