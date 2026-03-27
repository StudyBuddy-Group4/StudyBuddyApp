package com.example.studybuddyapp;

import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
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
public class CreateAccountActivityInstrumentedTest {

    @Rule
    public ActivityScenarioRule<CreateAccountActivity> activityRule =
            new ActivityScenarioRule<>(CreateAccountActivity.class);

    @Test
    public void signUp_withEmptyFields_keepsButtonEnabled() {
        activityRule.getScenario().onActivity(activity -> {
            Button signUpButton = activity.findViewById(R.id.btnSignUp);

            signUpButton.performClick();

            assertTrue(signUpButton.isEnabled());
        });
    }

    @Test
    public void signUp_withPasswordMismatch_keepsButtonEnabled() {
        activityRule.getScenario().onActivity(activity -> {
            EditText username = activity.findViewById(R.id.etUsername);
            EditText email = activity.findViewById(R.id.etEmail);
            EditText password = activity.findViewById(R.id.etPassword);
            EditText confirmPassword = activity.findViewById(R.id.etConfirmPassword);
            Button signUpButton = activity.findViewById(R.id.btnSignUp);

            username.setText("alice");
            email.setText("alice@example.com");
            password.setText("secret1");
            confirmPassword.setText("secret2");

            signUpButton.performClick();

            assertTrue(signUpButton.isEnabled());
        });
    }

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

    @Test
    public void confirmPasswordToggle_showsAndHidesPassword() {
        activityRule.getScenario().onActivity(activity -> {
            EditText confirmPassword = activity.findViewById(R.id.etConfirmPassword);
            ImageView toggle = activity.findViewById(R.id.ivToggleConfirmPassword);

            confirmPassword.setText("secret");
            toggle.performClick();
            assertFalse(confirmPassword.getTransformationMethod() instanceof PasswordTransformationMethod);

            toggle.performClick();
            assertTrue(confirmPassword.getTransformationMethod() instanceof PasswordTransformationMethod);
        });
    }

    @Test
    public void logInText_finishesActivity() {
        activityRule.getScenario().onActivity(activity -> {
            View logInText = activity.findViewById(R.id.tvLogIn);

            logInText.performClick();

            assertTrue(activity.isFinishing());
        });
    }

    @Test
    public void backButton_finishesActivity() {
        activityRule.getScenario().onActivity(activity -> {
            View backButton = activity.findViewById(R.id.ivBack);

            backButton.performClick();

            assertTrue(activity.isFinishing());
        });
    }
}
