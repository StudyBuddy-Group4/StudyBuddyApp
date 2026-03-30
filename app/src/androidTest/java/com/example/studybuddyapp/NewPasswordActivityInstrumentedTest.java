package com.example.studybuddyapp;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class NewPasswordActivityInstrumentedTest {

    private NewPasswordActivity activity;

    @After
    public void tearDown() {
        if (activity != null) {
            activity.finish();
        }
    }

    // test that both password fields start hidden and can be shown with their toggles
    @Test
    public void passwordToggles_showBothPasswordFields() {
        launchActivity();

        EditText newPassword = activity.findViewById(R.id.etNewPassword);
        EditText confirmPassword = activity.findViewById(R.id.etConfirmNewPassword);

        assertTrue(newPassword.getTransformationMethod() instanceof PasswordTransformationMethod);
        assertTrue(confirmPassword.getTransformationMethod() instanceof PasswordTransformationMethod);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            activity.findViewById(R.id.ivToggleNewPassword).performClick();
            activity.findViewById(R.id.ivToggleConfirmPassword).performClick();
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertTrue(newPassword.getTransformationMethod()
                instanceof HideReturnsTransformationMethod);
        assertTrue(confirmPassword.getTransformationMethod()
                instanceof HideReturnsTransformationMethod);
    }

    // test that the main action moves the flow to the password-changed screen
    @Test
    public void changePasswordClick_startsPasswordChangedActivity() {
        launchActivity();

        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                PasswordChangedActivity.class.getName(), null, false);

        instrumentation.runOnMainSync(() ->
                activity.findViewById(R.id.btnChangePassword).performClick());

        PasswordChangedActivity nextActivity =
                (PasswordChangedActivity) instrumentation.waitForMonitorWithTimeout(monitor, 3000);

        assertNotNull(nextActivity);
        nextActivity.finish();
    }

    private void launchActivity() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, NewPasswordActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        activity = (NewPasswordActivity) InstrumentationRegistry.getInstrumentation()
                .startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
