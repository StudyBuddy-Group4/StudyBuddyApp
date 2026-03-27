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
public class LoginActivityInstrumentedTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    @Test
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
}
