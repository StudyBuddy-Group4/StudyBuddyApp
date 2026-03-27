package com.example.studybuddyapp;

import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ChangePasswordActivityInstrumentedTest {

    @Rule
    public ActivityScenarioRule<ChangePasswordActivity> activityRule =
            new ActivityScenarioRule<>(ChangePasswordActivity.class);

    @Test
    // test that the current password field becomes visible when its toggle is pressed
    public void currentPasswordToggle_showsPasswordTextWhenClicked() {
        activityRule.getScenario().onActivity(activity -> {
            EditText current = activity.findViewById(R.id.etCurrentPassword);
            ImageView toggle = activity.findViewById(R.id.ivToggleCurrent);

            current.setText("secret");
            toggle.performClick();

            assertFalse(current.getTransformationMethod() instanceof PasswordTransformationMethod);
            assertEquals(current.length(), current.getSelectionStart());
            assertEquals(current.length(), current.getSelectionEnd());
        });
    }

    @Test
    // test that the new password field becomes hidden again after pressing its toggle twice
    public void newPasswordToggle_hidesPasswordAgainWhenClickedTwice() {
        activityRule.getScenario().onActivity(activity -> {
            EditText newPassword = activity.findViewById(R.id.etNewPassword);
            ImageView toggle = activity.findViewById(R.id.ivToggleNew);

            newPassword.setText("secret");
            toggle.performClick();
            toggle.performClick();

            assertTrue(newPassword.getTransformationMethod() instanceof PasswordTransformationMethod);
        });
    }

    @Test
    // test that the confirm password field can switch between visible and hidden text
    public void confirmPasswordToggle_showsAndHidesPassword() {
        activityRule.getScenario().onActivity(activity -> {
            EditText confirm = activity.findViewById(R.id.etConfirmPassword);
            ImageView toggle = activity.findViewById(R.id.ivToggleConfirm);

            confirm.setText("secret");
            toggle.performClick();
            assertFalse(confirm.getTransformationMethod() instanceof PasswordTransformationMethod);

            toggle.performClick();
            assertTrue(confirm.getTransformationMethod() instanceof PasswordTransformationMethod);
        });
    }
}
