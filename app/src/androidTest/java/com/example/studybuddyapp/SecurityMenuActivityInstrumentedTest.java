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
public class SecurityMenuActivityInstrumentedTest {

    private SecurityMenuActivity activity;

    @After
    public void tearDown() {
        if (activity != null) {
            activity.finish();
        }
    }

    // test that the page shows the expected security title and menu label
    @Test
    public void onCreate_displaysSecuritySectionContent() {
        launchActivity();

        TextView titleView = activity.findViewById(R.id.tvTitle);
        TextView menuLabel = findTextViewByText(activity.getString(R.string.change_password));

        assertEquals(activity.getString(R.string.security), titleView.getText().toString());
        assertNotNull(menuLabel);
    }

    // test that tapping the only menu row opens the change-password screen
    @Test
    public void menuChangePasswordClick_startsChangePasswordActivity() {
        launchActivity();

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                ChangePasswordActivity.class.getName(), null, false);

        instrumentation.runOnMainSync(() ->
                activity.findViewById(R.id.menuChangePassword).performClick());

        ChangePasswordActivity nextActivity = (ChangePasswordActivity)
                instrumentation.waitForMonitorWithTimeout(monitor, 3000);

        assertNotNull(nextActivity);
        nextActivity.finish();
    }

    private void launchActivity() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, SecurityMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        activity = (SecurityMenuActivity) InstrumentationRegistry.getInstrumentation()
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
