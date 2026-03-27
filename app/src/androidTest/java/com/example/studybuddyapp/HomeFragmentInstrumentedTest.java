package com.example.studybuddyapp;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class HomeFragmentInstrumentedTest {

    private LaunchOptionsActivity activity;
    private HomeFragment fragment;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, LaunchOptionsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        activity = (LaunchOptionsActivity) InstrumentationRegistry.getInstrumentation()
                .startActivitySync(intent);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            FrameLayout container = new FrameLayout(activity);
            container.setId(View.generateViewId());
            activity.setContentView(container);

            fragment = new HomeFragment();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(container.getId(), fragment)
                    .commitNow();
        });
    }

    @After
    public void tearDown() {
        if (activity != null) {
            activity.finish();
        }
    }

    @Test
    public void clickChip30_updatesSelectedDurationAndClearsCustomSelection() throws Exception {
        setField(fragment, "selectedDurationMinutes", 45);
        setField(fragment, "isCustomSelected", true);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                fragment.requireView().findViewById(R.id.chip_30_min).performClick());

        TextView customChip = fragment.requireView().findViewById(R.id.chip_custom);

        assertEquals(30, getIntField(fragment, "selectedDurationMinutes"));
        assertFalse(getBooleanField(fragment, "isCustomSelected"));
        assertEquals(fragment.getString(R.string.time_custom), customChip.getText().toString());
    }

    @Test
    public void clickChip15_updatesSelectedDurationAndClearsCustomSelection() throws Exception {
        setField(fragment, "selectedDurationMinutes", 60);
        setField(fragment, "isCustomSelected", true);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                fragment.requireView().findViewById(R.id.chip_15_min).performClick());

        TextView customChip = fragment.requireView().findViewById(R.id.chip_custom);

        assertEquals(15, getIntField(fragment, "selectedDurationMinutes"));
        assertFalse(getBooleanField(fragment, "isCustomSelected"));
        assertEquals(fragment.getString(R.string.time_custom), customChip.getText().toString());
    }

    @Test
    public void defaultState_startsWithFifteenMinutesSelected() throws Exception {
        assertEquals(15, getIntField(fragment, "selectedDurationMinutes"));
        assertFalse(getBooleanField(fragment, "isCustomSelected"));
    }

    @Test
    public void clickCustomChip_doesNotChangeSelectionUntilDialogConfirms() throws Exception {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                fragment.requireView().findViewById(R.id.chip_custom).performClick());

        assertEquals(15, getIntField(fragment, "selectedDurationMinutes"));
        assertFalse(getBooleanField(fragment, "isCustomSelected"));
    }

    @Test
    public void clickChip30_updatesChipTextColorsForSelectedState() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                fragment.requireView().findViewById(R.id.chip_30_min).performClick());

        TextView chip15 = fragment.requireView().findViewById(R.id.chip_15_min);
        TextView chip30 = fragment.requireView().findViewById(R.id.chip_30_min);
        TextView chipCustom = fragment.requireView().findViewById(R.id.chip_custom);

        int selectedColor = fragment.requireContext().getColor(R.color.white);
        int unselectedColor = fragment.requireContext().getColor(R.color.dark_text);

        assertEquals(unselectedColor, chip15.getCurrentTextColor());
        assertEquals(selectedColor, chip30.getCurrentTextColor());
        assertEquals(unselectedColor, chipCustom.getCurrentTextColor());
    }

    @Test
    public void clickChip15_restoresDefaultChipTextColors() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            fragment.requireView().findViewById(R.id.chip_30_min).performClick();
            fragment.requireView().findViewById(R.id.chip_15_min).performClick();
        });

        TextView chip15 = fragment.requireView().findViewById(R.id.chip_15_min);
        TextView chip30 = fragment.requireView().findViewById(R.id.chip_30_min);
        TextView chipCustom = fragment.requireView().findViewById(R.id.chip_custom);

        int selectedColor = fragment.requireContext().getColor(R.color.white);
        int unselectedColor = fragment.requireContext().getColor(R.color.dark_text);

        assertEquals(selectedColor, chip15.getCurrentTextColor());
        assertEquals(unselectedColor, chip30.getCurrentTextColor());
        assertEquals(unselectedColor, chipCustom.getCurrentTextColor());
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private int getIntField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(target);
    }

    private boolean getBooleanField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getBoolean(target);
    }
}
