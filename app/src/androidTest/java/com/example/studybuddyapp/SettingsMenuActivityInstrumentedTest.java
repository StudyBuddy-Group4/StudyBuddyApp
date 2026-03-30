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
public class SettingsMenuActivityInstrumentedTest {

    private SettingsMenuActivity activity;

    @After
    public void tearDown() {
        if (activity != null) {
            activity.finish();
        }
    }

    // test that the page shows the settings title and both menu labels
    @Test
    public void onCreate_displaysSettingsSectionContent() {
        launchActivity();

        TextView titleView = activity.findViewById(R.id.tvTitle);
        TextView notificationLabel =
                findTextViewByText(activity.getString(R.string.notification_settings));
        TextView deleteLabel =
                findTextViewByText(activity.getString(R.string.delete_account));

        assertEquals(activity.getString(R.string.settings_title), titleView.getText().toString());
        assertNotNull(notificationLabel);
        assertNotNull(deleteLabel);
    }

    // test that tapping notification settings opens the notification settings screen
    @Test
    public void menuNotificationSettingsClick_startsNotificationSettingsActivity() {
        launchActivity();

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                NotificationSettingsActivity.class.getName(), null, false);

        instrumentation.runOnMainSync(() ->
                activity.findViewById(R.id.menuNotificationSettings).performClick());

        NotificationSettingsActivity nextActivity = (NotificationSettingsActivity)
                instrumentation.waitForMonitorWithTimeout(monitor, 3000);

        assertNotNull(nextActivity);
        nextActivity.finish();
    }

    // test that tapping delete account opens the delete-account screen
    @Test
    public void menuDeleteAccountClick_startsDeleteAccountActivity() {
        launchActivity();

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                DeleteAccountActivity.class.getName(), null, false);

        instrumentation.runOnMainSync(() ->
                activity.findViewById(R.id.menuDeleteAccount).performClick());

        DeleteAccountActivity nextActivity = (DeleteAccountActivity)
                instrumentation.waitForMonitorWithTimeout(monitor, 3000);

        assertNotNull(nextActivity);
        nextActivity.finish();
    }

    private void launchActivity() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, SettingsMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        activity = (SettingsMenuActivity) InstrumentationRegistry.getInstrumentation()
                .startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private TextView findTextViewByText(String expectedText) {
        return findTextViewByText(activity.findViewById(android.R.id.content), expectedText);
    }

    private TextView findTextViewByText(android.view.View view, String expectedText) {
        if (view instanceof TextView
                && expectedText.equals(((TextView) view).getText().toString())) {
            return (TextView) view;
        }

        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findTextViewByText(group.getChildAt(i), expectedText);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }
}
