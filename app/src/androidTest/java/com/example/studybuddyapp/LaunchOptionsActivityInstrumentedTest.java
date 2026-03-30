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

    // test that the login button opens the login screen
    @Test
    public void loginClick_startsLoginActivity() {
        launchActivity();

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                LoginActivity.class.getName(), null, false);

        instrumentation.runOnMainSync(() ->
                activity.findViewById(R.id.btnLogIn).performClick());

        LoginActivity nextActivity =
                (LoginActivity) instrumentation.waitForMonitorWithTimeout(monitor, 3000);

        assertNotNull(nextActivity);
        nextActivity.finish();
    }

    // test that the sign-up button opens the account creation screen
    @Test
    public void signUpClick_startsCreateAccountActivity() {
        launchActivity();

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                CreateAccountActivity.class.getName(), null, false);

        instrumentation.runOnMainSync(() ->
                activity.findViewById(R.id.btnSignUp).performClick());

        CreateAccountActivity nextActivity =
                (CreateAccountActivity) instrumentation.waitForMonitorWithTimeout(monitor, 3000);

        assertNotNull(nextActivity);
        nextActivity.finish();
    }

    // test that the forgot-password link opens the recovery screen
    @Test
    public void forgotPasswordClick_startsForgotPasswordActivity() {
        launchActivity();

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                ForgotPasswordActivity.class.getName(), null, false);

        instrumentation.runOnMainSync(() ->
                activity.findViewById(R.id.tvForgotPassword).performClick());

        ForgotPasswordActivity nextActivity =
                (ForgotPasswordActivity) instrumentation.waitForMonitorWithTimeout(monitor, 3000);

        assertNotNull(nextActivity);
        nextActivity.finish();
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
