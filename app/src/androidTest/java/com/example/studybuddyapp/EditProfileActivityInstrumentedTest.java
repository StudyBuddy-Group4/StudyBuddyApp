package com.example.studybuddyapp;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class EditProfileActivityInstrumentedTest {

    @Rule
    public ActivityScenarioRule<EditProfileActivity> activityRule =
            new ActivityScenarioRule<>(EditProfileActivity.class);

    @Test
    // test that leaving both fields empty does not close the activity
    public void updateProfile_withEmptyFields_keepsActivityActive() {
        activityRule.getScenario().onActivity(activity -> {
            EditText username = activity.findViewById(R.id.etUsername);
            EditText email = activity.findViewById(R.id.etEmail);

            username.setText("");
            email.setText("");
            activity.findViewById(R.id.btnUpdateProfile).performClick();

            assertFalse(activity.isFinishing());
        });
    }

    @Test
    // test that a cached username and id can be shown in the editable profile screen
    public void cachedSessionData_canBeDisplayedInProfileFields() {
        activityRule.getScenario().onActivity(activity -> {
            SessionManager session = new SessionManager(activity);
            session.saveLoginSession("token-1", 42L, "alice", false);

            EditText username = activity.findViewById(R.id.etUsername);
            TextView headerName = activity.findViewById(R.id.tvHeaderName);
            TextView headerId = activity.findViewById(R.id.tvHeaderId);

            username.setText(session.getUsername());
            headerName.setText(session.getUsername());
            headerId.setText("ID: " + session.getUserId());

            assertEquals("alice", username.getText().toString());
            assertEquals("alice", headerName.getText().toString());
            assertEquals("ID: 42", headerId.getText().toString());
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
