package com.example.studybuddyapp;

import android.text.method.PasswordTransformationMethod;
import android.view.View;
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
public class DeleteAccountActivityInstrumentedTest {

    @Rule
    public ActivityScenarioRule<DeleteAccountActivity> activityRule =
            new ActivityScenarioRule<>(DeleteAccountActivity.class);

    @Test
    // test that submitting deletion without a password keeps the user on the same screen
    public void deleteAccount_withEmptyPassword_keepsActivityActive() {
        activityRule.getScenario().onActivity(activity -> {
            activity.findViewById(R.id.btnDeleteAccount).performClick();

            assertFalse(activity.isFinishing());
        });
    }

    @Test
    // test that the password field becomes visible when its toggle is pressed
    public void passwordToggle_showsPasswordTextWhenClicked() {
        activityRule.getScenario().onActivity(activity -> {
            EditText password = activity.findViewById(R.id.etPassword);
            ImageView toggle = activity.findViewById(R.id.ivTogglePassword);

            password.setText("secret");
            toggle.performClick();

            assertFalse(password.getTransformationMethod() instanceof PasswordTransformationMethod);
            assertEquals(password.length(), password.getSelectionStart());
            assertEquals(password.length(), password.getSelectionEnd());
        });
    }

    @Test
    // test that the password field becomes hidden again after pressing the toggle twice
    public void passwordToggle_hidesPasswordAgainWhenClickedTwice() {
        activityRule.getScenario().onActivity(activity -> {
            EditText password = activity.findViewById(R.id.etPassword);
            ImageView toggle = activity.findViewById(R.id.ivTogglePassword);

            password.setText("secret");
            toggle.performClick();
            toggle.performClick();

            assertTrue(password.getTransformationMethod() instanceof PasswordTransformationMethod);
        });
    }

    @Test
    // test that tapping cancel closes the activity without deleting the account
    public void cancelButton_finishesActivity() {
        activityRule.getScenario().onActivity(activity -> {
            View cancelButton = activity.findViewById(R.id.btnCancel);

            cancelButton.performClick();

            assertTrue(activity.isFinishing());
        });
    }

    @Test
    // test that tapping the back button closes the activity
    public void backButton_finishesActivity() {
        activityRule.getScenario().onActivity(activity -> {
            View backButton = activity.findViewById(R.id.ivBack);

            backButton.performClick();

            assertTrue(activity.isFinishing());
        });
    }
}
