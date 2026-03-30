package com.example.studybuddyapp;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
public class ForgotPasswordActivityInstrumentedTest {

    private ForgotPasswordActivity activity;

    @After
    public void tearDown() {
        if (activity != null) {
            activity.finish();
        }
    }

    // test that the screen shows the recovery prompt and explanation text
    @Test
    public void onCreate_displaysRecoveryTexts() {
        launchActivity();

        TextView titleView = activity.findViewById(R.id.tvTitle);
        TextView promptView = findTextViewByText(activity.getString(R.string.reset_password_question));
        TextView descriptionView = findTextViewByText(activity.getString(R.string.reset_password_desc));

        assertEquals(activity.getString(R.string.forgot_password_title), titleView.getText().toString());
        assertNotNull(promptView);
        assertNotNull(descriptionView);
    }

    // test that the email field starts empty and the next-step button is visible
    @Test
    public void onCreate_startsWithEmptyEmailFieldAndVisibleAction() {
        launchActivity();

        EditText emailField = activity.findViewById(R.id.etEmail);
        Button nextButton = activity.findViewById(R.id.btnNextStep);

        assertEquals("", emailField.getText().toString());
        assertEquals(Button.VISIBLE, nextButton.getVisibility());
    }

    // test that the next-step button opens the security-code screen
    @Test
    public void nextStepClick_startsSecurityCodeActivity() {
        launchActivity();

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                SecurityCodeActivity.class.getName(), null, false);

        instrumentation.runOnMainSync(() ->
                activity.findViewById(R.id.btnNextStep).performClick());

        SecurityCodeActivity nextActivity =
                (SecurityCodeActivity) instrumentation.waitForMonitorWithTimeout(monitor, 3000);

        assertNotNull(nextActivity);
        nextActivity.finish();
    }

    private void launchActivity() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, ForgotPasswordActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        activity = (ForgotPasswordActivity) InstrumentationRegistry.getInstrumentation()
                .startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private TextView findTextViewByText(String expectedText) {
        View root = activity.findViewById(android.R.id.content);
        return findTextViewByText(root, expectedText);
    }

    private TextView findTextViewByText(View view, String expectedText) {
        if (view instanceof TextView
                && expectedText.equals(((TextView) view).getText().toString())) {
            return (TextView) view;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
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
