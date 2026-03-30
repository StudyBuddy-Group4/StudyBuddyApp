package com.example.studybuddyapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.view.ViewGroup;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class MainActivityTest {

    @Test
    public void onCreate_setsMainLayoutAndPlaceholderText() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        ViewGroup root = activity.findViewById(R.id.main);
        assertNotNull(root);

        TextView textView = (TextView) root.getChildAt(0);
        assertEquals("Hello World!", textView.getText().toString());
    }
}
