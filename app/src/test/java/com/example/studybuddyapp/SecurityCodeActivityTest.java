package com.example.studybuddyapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.widget.EditText;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SecurityCodeActivityTest {

    @Test
    public void onCreate_prefillsMockSecurityCode() {
        SecurityCodeActivity activity =
                Robolectric.buildActivity(SecurityCodeActivity.class).setup().get();

        assertEquals("2", ((EditText) activity.findViewById(R.id.etCode1)).getText().toString());
        assertEquals("7", ((EditText) activity.findViewById(R.id.etCode2)).getText().toString());
        assertEquals("3", ((EditText) activity.findViewById(R.id.etCode3)).getText().toString());
        assertEquals("9", ((EditText) activity.findViewById(R.id.etCode4)).getText().toString());
        assertEquals("1", ((EditText) activity.findViewById(R.id.etCode5)).getText().toString());
        assertEquals("6", ((EditText) activity.findViewById(R.id.etCode6)).getText().toString());
    }

    @Test
    public void confirmButton_startsNewPasswordActivity() {
        SecurityCodeActivity activity =
                Robolectric.buildActivity(SecurityCodeActivity.class).setup().get();

        activity.findViewById(R.id.btnConfirm).performClick();

        Intent nextIntent = shadowOf(activity).getNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(NewPasswordActivity.class.getName(), nextIntent.getComponent().getClassName());
    }
}
